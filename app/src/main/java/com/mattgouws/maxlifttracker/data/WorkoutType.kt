package com.mattgouws.maxlifttracker.data

enum class WorkoutType {
    /** Part of the repeating rotation, done in [Workout.rotationOrder]. */
    MAIN,

    /** Done only when queued in between main workouts. */
    ADHOC,
}
