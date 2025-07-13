package zionomicon.ch12_hub

import cats.syntax.option.*
import io.github.iltotore.iron.{Pure, RefinedType}
import zio.*
import zio.stm.*
import zionomicon.ch12_hub.Ch12_Ex2_AuctionSystem.AuctionEvent.*

import java.lang.{IllegalArgumentException as IAE, IllegalStateException as ISE}
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

/**
 * Write a real-time auction system that allows multiple bidders to practice in auctions
 * simultaneously. The auction system should broadcast bid updates to all participants
 * while maintaining the strict ordering of bids. Each participant should be able to
 * place bids and receive updates on the current highest bid.
 */
object Ch12_Ex2_AuctionSystem:

  class AuctionSystem(
    auctions: TMap[AuctionId, Auction],
    bids: TMap[AuctionId, Chunk[Bid]],
    eventHub: THub[AuctionEvent]
  ):
    def placeBid(
      auctionId: AuctionId,
      bidderId: BidderId,
      amount: BigDecimal
    ): Task[Boolean] =
      for
        now <- Clock.currentDateTime.map(_.atZoneSameInstant(UTC))
        bid = Bid(auctionId, bidderId, amount, now)
        isPublished <- STM.atomically {
          for
            case Some(bs) <- bids.updateWithSTM(auctionId) {
              case Some(currentBids) if currentBids.nonEmpty && bid.amount <= currentBids.last.amount =>
                STM.fail(IAE(s"New bid for auction $auctionId must be higher than highest bid, but " +
                  s"${bid.amount} <= ${currentBids.last.amount}"))
              case maybeBids =>
                STM.succeed(Some(maybeBids.getOrElse(Chunk.empty) :+ bid))
            }
            _ <- auctions.updateWithSTM(auctionId) {
              case Some(auction) if auction.isActive =>
                val currentWinner = bs.maxByOption(_.amount)
                val updated = auction.copy(
                  currentPrice = amount,
                  currentWinner = currentWinner.map(_.bidderId)
                ).some
                STM.succeed(updated)
              case Some(auction) =>
                STM.fail(ISE(s"Auction $auctionId is already closed"))
              case None =>
                STM.fail(ISE(s"Auction $auctionId does not exist"))
            }
            isPublished <- eventHub.publish(BidPlaced(bid))
          yield isPublished
        }
      yield isPublished

    def createAuction(
      id: AuctionId,
      startPrice: BigDecimal,
      duration: Duration
    ): Task[Unit] =
      for
        now <- Clock.currentDateTime.map(_.atZoneSameInstant(UTC))
        auction = Auction(id, startPrice, None, now.plus(duration), true)
        _ <- STM.atomically {
          STM.whenSTM(auctions.contains(id))(STM.fail(ISE(s"Auction $id already exists"))) *>
            bids.put(id, Chunk.empty) *>
            auctions.put(id, auction)
        }
      yield ()

    def subscribe: URIO[Scope, TDequeue[AuctionEvent]] =
      eventHub.subscribe.commit

    def getAuction(id: AuctionId): UIO[Option[Auction]] =
      auctions.get(id).commit

    def endAuction(id: AuctionId): Task[Unit] =
      for
        now <- Clock.currentDateTime.map(_.atZoneSameInstant(UTC))
        _ <- STM.atomically:
          for
            case Some(auction) <- auctions.updateWithSTM(id) {
              case Some(auction) =>
                STM.succeed(auction.copy(
                  isActive = false
                ).some)
              case None =>
                STM.fail(ISE(s"Auction '$id' does not exist"))
            }
            _ <- eventHub.publish(AuctionEnded(id, auction.currentPrice, auction.currentWinner))
          yield ()
      yield ()

  object AuctionSystem:
    def make: UIO[AuctionSystem] =
      STM.atomically:
        for
          auctions <- TMap.empty[AuctionId, Auction]
          bids <- TMap.empty[AuctionId, Chunk[Bid]]
          eventHub <- THub.unbounded[AuctionEvent]
        yield AuctionSystem(auctions, bids, eventHub)

  type AuctionId = AuctionId.T
  object AuctionId extends RefinedType[String, Pure]

  type BidderId = BidderId.T
  object BidderId extends RefinedType[String, Pure]

  case class Bid(
    auctionId: AuctionId,
    bidderId: BidderId,
    amount: BigDecimal,
    timestamp: ZonedDateTime
  )

  case class Auction(
    id: AuctionId,
    currentPrice: BigDecimal,
    currentWinner: Option[BidderId],
    endTime: ZonedDateTime,
    isActive: Boolean
  )

  sealed trait AuctionEvent

  object AuctionEvent:
    case class BidPlaced(
      bid: Bid
    ) extends AuctionEvent

    case class AuctionEnded(
      auctionId: AuctionId,
      finalPrice: BigDecimal,
      winner: Option[BidderId]
    ) extends AuctionEvent
