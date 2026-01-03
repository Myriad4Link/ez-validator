package xyz.uthofficial.ezvalidator.states.errors

data class TooLessPresentedError(val required: Int, val presented: Int) : ValidationError