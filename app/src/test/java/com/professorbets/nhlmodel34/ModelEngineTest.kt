package com.professorbets.nhlmodel34

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelEngineTest {
    @Test
    fun neutralTeamsStillGetHomeIceBaseline() {
        val p = ModelEngine.predict(
            ModelEngine.Inputs(
                1500.0,1500.0,false,false,.5,.5,.5,.5,.5,.5,0.0,0.0
            )
        )
        assertTrue(p.homeProbability > .50)
        assertEquals(1.0, p.homeProbability + p.awayProbability, 1e-9)
    }

    @Test
    fun strongerHomeTeamGetsHigherProbability() {
        val neutral = ModelEngine.predict(ModelEngine.Inputs(1500.0,1500.0,false,false,.5,.5,.5,.5,.5,.5,0.0,0.0))
        val strong = ModelEngine.predict(ModelEngine.Inputs(1600.0,1450.0,false,false,.56,.46,.55,.47,.57,.45,.30,-.20))
        assertTrue(strong.homeProbability > neutral.homeProbability)
    }
}
