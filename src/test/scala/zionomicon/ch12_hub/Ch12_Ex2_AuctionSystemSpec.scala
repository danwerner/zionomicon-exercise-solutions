package zionomicon.ch12_hub

import zio.*
import zio.test.Assertion.*
import zio.test.*
import zionomicon.ch12_hub.Ch12_Ex2_AuctionSystem.*
import zionomicon.ch12_hub.Ch12_Ex2_AuctionSystem.AuctionEvent.*

import java.time.ZoneOffset.UTC

object Ch12_Ex2_AuctionSystemSpec extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Chapter 12, Ex 2: AuctionSystem")(
      test("places bids, rejects invalid bids, keeps bid order, determines correct winner") {
        for {
          start <- Clock.instant.map(_.atZone(UTC))
          system <- AuctionSystem.make
          subscriber <- system.subscribe

          auctionId = AuctionId("auction1")
          bidder1 = BidderId("bidder1")
          bidder2 = BidderId("bidder2")
          bidder3 = BidderId("bidder3")

          auctionDuration = 2.hours
          step = 1.hours

          // create auction at start
          _ <- system.createAuction(auctionId, BigDecimal(100), auctionDuration)
          expectedEnd = start.plus(auctionDuration)

          // place first bid after 1h
          _ <- TestClock.adjust(step)
          ok1 <- system.placeBid(auctionId, bidder1, BigDecimal(150))
          e1 <- subscriber.take.commit

          // place second bid after 1h (total +2h)
          _ <- TestClock.adjust(step)
          ok2 <- system.placeBid(auctionId, bidder2, BigDecimal(200))
          e2 <- subscriber.take.commit

          // attempt invalid bid (amount == existing bid) after 1h (total +3h)
          _ <- TestClock.adjust(step)
          invalid <- system.placeBid(auctionId, bidder3, BigDecimal(200)).either

          // place valid third bid after 1h (total +4h)
          _ <- TestClock.adjust(step)
          ok3 <- system.placeBid(auctionId, bidder3, BigDecimal(250))
          e3 <- subscriber.take.commit

          // end the auction
          _ <- system.endAuction(auctionId)
          eEnd <- subscriber.take.commit

          // fetch final state
          finalState <- system.getAuction(auctionId)
        } yield {
          val bids = List(e1, e2, e3).collect { case BidPlaced(b) => b }

          assertTrue(
            // valid bids succeed
            ok1, ok2, ok3,

            // verify amounts and bidders order
            bids.map(_.amount) == List(BigDecimal(150), BigDecimal(200), BigDecimal(250)),
            bids.map(_.bidderId) == List(bidder1, bidder2, bidder3),

            // timestamps correspond to start +1h, +2h, +4h
            bids.map(_.timestamp) == List(
              start.plus(step),
              start.plus(step.multipliedBy(2)),
              start.plus(step.multipliedBy(4))
            ),

            // AuctionEnded event is correct
            eEnd match {
              case AuctionEnded(id, price, winner) =>
                id == auctionId && price == BigDecimal(250) && winner.contains(bidder3)
              case _ => false
            },

            // final auction state: inactive, correct winner & price, correct endTime
            finalState.exists(a =>
              !a.isActive &&
                a.currentPrice == BigDecimal(250) &&
                a.currentWinner.contains(bidder3) &&
                a.endTime == expectedEnd
            )
          ) &&
            // invalid bid is rejected
            assert(invalid)(isLeft(hasMessage(endsWithString("must be higher than highest bid, but 200 <= 200"))))
        }
      }
    )
}
