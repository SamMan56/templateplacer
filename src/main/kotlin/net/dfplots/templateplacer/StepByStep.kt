package net.dfplots.templateplacer

enum class Result {
    SUCCESS, KEEP_TRYING, FAIL
}

typealias Work = () -> Result

var currentStepByStep: StepByStep? = null

class StepByStep(val queue: MutableList<Work> = mutableListOf()) {
    fun tryTo(work: Work) {
        queue.add(work)
    }

    internal fun run() {
        if (currentStepByStep == null)
            currentStepByStep = this
    }
}

fun stepByStep(f: StepByStep.() -> Unit) {
    val stepByStep = StepByStep()
    stepByStep.f()
    stepByStep.run()
}

fun tick() {
    val checkStepByStep = currentStepByStep
    if (checkStepByStep != null) {
        if (checkStepByStep.queue.size > 0) {
            val task = checkStepByStep.queue[0]

            val result = task()

            when (result) {
                Result.SUCCESS -> checkStepByStep.queue.removeAt(0)
                Result.KEEP_TRYING -> {} // try again next tick
                Result.FAIL -> currentStepByStep = null
            }
        } else {
            currentStepByStep = null
        }
    }
}