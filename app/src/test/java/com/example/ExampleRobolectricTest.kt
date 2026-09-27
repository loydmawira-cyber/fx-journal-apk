package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TradeEntity
import com.example.model.Trade
import com.example.model.TradeDirection
import com.example.model.TradeVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FX Journal", appName)
    }

    @Test
    fun `verify trade entity domain conversion`() {
        val trade = Trade(
            id = 42,
            pair = "EUR/USD",
            direction = TradeDirection.LONG,
            setupStrategy = "SMC Order Block",
            entryPrice = 1.08420,
            stopLoss = 1.08200,
            takeProfit = 1.09210,
            rMultiple = 3.8,
            netGainDollars = 4520.0,
            visibility = TradeVisibility.PUBLIC
        )

        val entity = TradeEntity.fromDomain(trade)
        val domain = entity.toDomain()

        assertEquals(42L, domain.id)
        assertEquals("EUR/USD", domain.pair)
        assertEquals(TradeDirection.LONG, domain.direction)
        assertEquals(3.8, domain.rMultiple, 0.001)
        assertEquals(TradeVisibility.PUBLIC, domain.visibility)
    }
}
