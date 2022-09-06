package net.dfplots.templateplacer

enum class Result {
    SUCCESS, KEEP_TRYING, FAIL
}

//typealias Work = () -> Result

var currentTask: Task? = null

sealed interface Task {
    fun tick(): Result
}

class Work(val f: () -> Result) : Task {
    override fun tick(): Result = f()
}

class StepByStep(val f: StepByStep.() -> Unit): Task {
    var generatedTasks = false
    val tasks: MutableList<Task> = mutableListOf()

    override fun tick(): Result {
        if (!generatedTasks) {
            f()
            generatedTasks = true
        }

        return if (tasks.size > 0) {
            val result = tasks[0].tick()

            when (result) {
                Result.SUCCESS -> {
                    tasks.removeAt(0)
                    Result.KEEP_TRYING
                }
                else -> result
            }
        }
        else Result.SUCCESS
    }

    private fun add(task: Task) {
        tasks.add(task)
    }

    fun work(f: () -> Result) = add(Work(f))

    fun steps(f: StepByStep.() -> Unit) = add(StepByStep(f))
}

fun Task.start() {
    currentTask = this
}

fun tick() {
    val checkTask = currentTask

    if (checkTask != null) {
        val result = checkTask.tick()

        when (result) {
            Result.SUCCESS, Result.FAIL -> currentTask = null
            Result.KEEP_TRYING -> {}
        }
    }
}