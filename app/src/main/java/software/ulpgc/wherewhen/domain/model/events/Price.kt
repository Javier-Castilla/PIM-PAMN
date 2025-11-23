package software.ulpgc.wherewhen.domain.model.events

data class Price(
    val min: Double?,
    val max: Double?,
    val currency: String,
    val isFree: Boolean
) {
    fun formatPrice(): String {
        return when {
            isFree -> "Free"
            min != null && max != null && min == max -> "$min $currency"
            min != null && max != null -> "$min - $max $currency"
            min != null -> "From $min $currency"
            else -> "Price not available"
        }
    }
    
    companion object {
        fun free() = Price(null, null, "EUR", true)
        fun single(amount: Double, currency: String = "EUR") = 
            Price(amount, amount, currency, false)
        fun range(min: Double, max: Double, currency: String = "EUR") = 
            Price(min, max, currency, false)
    }
}
