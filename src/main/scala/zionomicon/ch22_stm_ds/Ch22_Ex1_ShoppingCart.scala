package zionomicon.ch22_stm_ds

import io.github.iltotore.iron.{Pure, RefinedType}
import zio.stm.*

import java.lang.{IllegalArgumentException as IAE, IllegalStateException as ISE}

/**
 * Write a shopping cart application using ZIO STM that each cart item has a key and
 * quantity. The application should support the following operations in concurrent
 * environment:
 *  - Add an item to the cart (addItem)
 *  - Remove an item from the cart (remove)
 *  - Update the quantity of an item in the cart (update)
 *  - Calculate the total price of the items in the cart (checkout)
 *
 * Hint: Use a TMap to store the items in the shopping cart, where the key is a String
 * representing the item’s key, and the value is an Item that includes the item’s price
 * and quantity.
 */
object Ch22_Ex1_ShoppingCart:
  opaque type Key <: String = String
  object Key extends RefinedType[Key, Pure]

  case class Item(price: Double, quantity: Int)

  case class ShoppingCart(items: TMap[Key, Item])

  object ShoppingCartService:

    def addItem(cart: ShoppingCart)(key: Key, item: Item): STM[ISE, Unit] =
      STM.ifSTM(cart.items.contains(key))(
        STM.fail(ISE(s"Item with key '$key' already present")),
        cart.items.put(key, item))

    def removeItem(cart: ShoppingCart)(key: Key): USTM[Unit] =
      cart.items.delete(key)

    def updateItem(cart: ShoppingCart)(key: Key, addQuantity: Int): STM[IAE | ISE, Unit] =
      for
        // NOTE: Could also be done with cart.items.updateSTM, but this works as well
        // because we are transactionally safe
        item <- cart.items.getOrElseSTM(key, STM.fail(ISE(s"Item with key '$key' not in cart")))
        newQuantity = item.quantity + addQuantity
        _ <- STM.when(newQuantity < 0)(STM.fail(IAE(s"Item '$key' quantity too low to remove ${-addQuantity}")))
        _ <- cart.items.put(key, item.copy(quantity = newQuantity))
      yield ()

    def checkout(cart: ShoppingCart): USTM[Double] =
      cart.items.values.map(_.map(i => i.price * i.quantity).sum)
