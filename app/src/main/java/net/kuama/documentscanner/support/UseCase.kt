package net.kuama.documentscanner.support

abstract class UseCase<Type, in Params> where Type : Any? {

    abstract suspend fun run(params: Params): Either<Failure, Type>

    suspend operator fun invoke(params: Params, onResult: (Either<Failure, Type>) -> Unit = {}) =
        onResult(run(params))
}

abstract class InfallibleUseCase<Type, in Params> where Type : Any? {
    abstract suspend fun run(params: Params): Type

    suspend operator fun invoke(params: Params, onResult: (Type) -> Unit = {}) =
        onResult(run(params))
}
