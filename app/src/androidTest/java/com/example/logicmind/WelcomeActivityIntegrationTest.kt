package com.example.logicmind

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logicmind.activities.WelcomeActivity
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeActivityIntegrationTest {

    //uruchamia WelcomeActivity
    @get:Rule
    val activityRule = ActivityScenarioRule(WelcomeActivity::class.java)

    private val TEST_EMAIL = "m.@gmail.com"
    private val TEST_PASSWORD = "Haslo1234"


    /**
     * onView - znajduje ten widok
     * withId - identyfikuje ten widok
     * perform - wykonuje akcję
     * typetext - wypisuje tekst do pola
     * closeSoftKeyboard - symuluje ze user ukryl klawiature
     */

    @Test
    fun test_login_invalidCredentials_showsError() {
        //niepoprawne hasło
        onView(withId(R.id.etEmail))
            .perform(typeText(TEST_EMAIL), closeSoftKeyboard())

        onView(withId(R.id.etPassword))
            .perform(typeText("Zlehaslo123"), closeSoftKeyboard())


        onView(withId(R.id.btnLogin))
            .perform(click())

        //błąd logowania powinien być widoczny
        onView(withId(R.id.tvErrorMessage))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.login_validation)))
    }

    @Test
    fun test_login_successfulNavigationToMain() {
        //istniejące konto
        onView(withId(R.id.etEmail))
            .perform(typeText(TEST_EMAIL), closeSoftKeyboard())

        onView(withId(R.id.etPassword))
            .perform(typeText(TEST_PASSWORD), closeSoftKeyboard())

        onView(withId(R.id.btnLogin))
            .perform(click())

        //bez błędu logowania
        onView(withId(R.id.tvErrorMessage))
            .check(matches(not(isDisplayed())))
    }

//    @Before
//    fun setupIntents() {
//        Intents.init()
//    }
//
//    @After
//    fun cleanupIntents() {
//        Intents.release()
//    }

    @Test
    fun test_registration_invalidEmail() {
        val INVALID_EMAIL = "zlyemail"
        val PASSWORD = "Haslo1234" //poprawne haslo

        // dobre haslo
        onView(withId(R.id.etPassword))
            .perform(typeText(PASSWORD), closeSoftKeyboard())

        //zły email
        onView(withId(R.id.etEmail))
            .perform(typeText(INVALID_EMAIL), closeSoftKeyboard())

        // klikanie rejestracji
        onView(withId(R.id.btnRegister))
            .perform(click())

        //sprawdza czy sie pokazuje username dialog
        onView(withId(R.id.etUsername))
            .check(doesNotExist())


        // sprawdza, czy WelcomeActivity nadal jest na ekranie
        onView(withId(R.id.btnRegister))
            .check(matches(isDisplayed()))
    }
}