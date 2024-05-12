package sequence

class SuppliedSequence<T>(
    private val supplier: () -> Sequence<T>
) : Sequence<T> {

    override fun iterator(): Iterator<T>  = supplier().iterator()
}

fun <T> supplySequence(supplier: () -> Sequence<T>): Sequence<T> = SuppliedSequence(supplier)