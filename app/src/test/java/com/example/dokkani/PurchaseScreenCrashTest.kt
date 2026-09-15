package com.example.dokkani

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.screens.purchase.PurchaseScreen
import com.example.dokkani.ui.screens.purchase.PurchaseViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PurchaseScreenCrashTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPurchaseViewModelInit() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = PurchaseViewModel(app)
    }

    @Test
    fun testPurchaseScreenRender() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = PurchaseViewModel(app)
        composeTestRule.setContent {
            PurchaseScreen(
                currentUserRole = UserRole.ADMIN,
                viewModel = vm
            )
        }
    }

    @Test
    fun testPurchaseScreenWithItemsAndDialogs() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = PurchaseViewModel(app)

        val dummyProduct = com.example.dokkani.data.local.entities.ProductEntity(
            id = 1L,
            name = "أرز بسمتي",
            code = "RICE-01",
            category = "مواد غذائية"
        )
        val dummyUnit = com.example.dokkani.data.local.entities.ProductUnitEntity(
            id = 10L,
            productId = 1L,
            unitName = "كيس 10كجم",
            conversionFactor = 1.0,
            costPrice = 45.0,
            sellingPrice = 60.0,
            barcode = "123456789",
            isBaseUnit = true
        )

        vm.addProductItem(dummyProduct, dummyUnit, 5.0, 45.0)

        composeTestRule.setContent {
            PurchaseScreen(
                currentUserRole = UserRole.ADMIN,
                viewModel = vm
            )
        }

        composeTestRule.waitForIdle()
    }
}
