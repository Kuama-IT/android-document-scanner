package net.kuama.documentscanner.support

sealed class Either<L, R> {

    abstract fun fold(left: (L) -> Unit, right: (R) -> Unit)

    abstract fun <ML> mapLeft(f: (L) -> ML): Either<ML, R>

    abstract fun <MR> mapRight(f: (R) -> MR): Either<L, MR>

    abstract fun <ML, MR> map(leftF: (L) -> ML, rightF: (R) -> MR): Either<ML, MR>

    abstract fun <ML> flatMapLeft(f: (L) -> Either<ML, R>): Either<ML, R>

    abstract fun <MR> flatMapRight(f: (R) -> Either<L, MR>): Either<L, MR>

    abstract fun <ML, MR> flatMap(leftF: (L) -> Either<ML, MR>, rightF: (R) -> Either<ML, MR>): Either<ML, MR>

    abstract fun filterLeft(filter: (L) -> Boolean, supplier: () -> R): Either<L, R>

    abstract fun filterRight(filter: (R) -> Boolean, supplier: () -> L): Either<L, R>
}

data class Left<L, R>(
    private val value: L
) : Either<L, R>() {

    override fun fold(left: (L) -> Unit, right: (R) -> Unit) = left(value)

    override fun <ML> mapLeft(f: (L) -> ML) =
        Left<ML, R>(f(value))

    override fun <MR> mapRight(f: (R) -> MR) =
        Left<L, MR>(value)

    override fun <ML, MR> map(leftF: (L) -> ML, rightF: (R) -> MR) =
        Left<ML, MR>(leftF(value))

    override fun <ML> flatMapLeft(f: (L) -> Either<ML, R>) = f(value)

    override fun <MR> flatMapRight(f: (R) -> Either<L, MR>) =
        Left<L, MR>(value)

    override fun <ML, MR> flatMap(leftF: (L) -> Either<ML, MR>, rightF: (R) -> Either<ML, MR>) = leftF(value)

    override fun filterLeft(filter: (L) -> Boolean, supplier: () -> R) = if (filter(value)) this else Right(
        supplier()
    )

    override fun filterRight(filter: (R) -> Boolean, supplier: () -> L) = this
}

data class Right<L, R>(
    private val value: R
) : Either<L, R>() {

    override fun fold(left: (L) -> Unit, right: (R) -> Unit) = right(value)

    override fun <ML> mapLeft(f: (L) -> ML) =
        Right<ML, R>(value)

    override fun <MR> mapRight(f: (R) -> MR) =
        Right<L, MR>(f(value))

    override fun <ML, MR> map(leftF: (L) -> ML, rightF: (R) -> MR) =
        Right<ML, MR>(rightF(value))

    override fun <ML> flatMapLeft(f: (L) -> Either<ML, R>) =
        Right<ML, R>(value)

    override fun <MR> flatMapRight(f: (R) -> Either<L, MR>) = f(value)

    override fun <ML, MR> flatMap(leftF: (L) -> Either<ML, MR>, rightF: (R) -> Either<ML, MR>) = rightF(value)

    override fun filterLeft(filter: (L) -> Boolean, supplier: () -> R) = this

    override fun filterRight(filter: (R) -> Boolean, supplier: () -> L) = if (filter(value)) this else Left(
        supplier()
    )
}
