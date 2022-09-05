package net.dfplots.templateplacer

enum class Result {
    SUCCESS, KEEP_TRYING, FAIL
}

typealias Work = () -> Result
typealias Plan = TaskSet.() -> Unit

var currentStepByStep: StepByStep? = null
var currentTaskSet: TaskSet? = null

class StepByStep(val plans: MutableList<Plan> = mutableListOf()) {
    internal fun run() {
        if (currentStepByStep == null)
            currentStepByStep = this
    }

    fun addTasks(plan: Plan) {
        plans.add(plan)
    }

    fun tryTo(work: Work) {
        addTasks {
            tryTo(work)
        }
    }
}

class TaskSet(val queue: MutableList<Work> = mutableListOf()) {
    fun tryTo(work: Work) {
        queue.add(work)
    }
}

fun stepByStep(f: StepByStep.() -> Unit) {
    val stepByStep = StepByStep()
    stepByStep.f()
    stepByStep.run()
}

fun tick() {
    val checkStepByStep = currentStepByStep
    val checkTaskSet = currentTaskSet
    if (checkStepByStep != null) {
        if (checkTaskSet != null) {
            if (checkTaskSet.queue.size > 0) {
                val work = checkTaskSet.queue[0]

                val result = work()

                when (result) {
                    Result.SUCCESS -> checkTaskSet.queue.removeAt(0)
                    Result.KEEP_TRYING -> {}
                    Result.FAIL -> currentStepByStep = null
                }

                if (checkTaskSet.queue.size == 0) currentTaskSet = null
            }
        } else {
            if (checkStepByStep.plans.size > 0) {
                val plan = checkStepByStep.plans[0]

                val taskSet = TaskSet()
                taskSet.plan()

                currentTaskSet = taskSet

                checkStepByStep.plans.removeAt(0)
            } else {
                currentStepByStep = null
            }
        }
    }
}