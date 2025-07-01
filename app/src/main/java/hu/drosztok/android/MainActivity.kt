package hu.drosztok.android

// Hozzáadtuk a hiányzó Intent importját
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val usernameInput: TextInputEditText = findViewById(R.id.login_username)
        val passwordInput: TextInputEditText = findViewById(R.id.login_password)
        val loginButton: Button = findViewById(R.id.login_button)
        val errorMessageTextView: TextView = findViewById(R.id.error_message_textview)
        val switchToRegisterTextView: TextView = findViewById(R.id.switch_to_register_text)

        // A bejelentkező gomb eseménykezelője (ez már megvolt)
        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isBlank() || password.isBlank()) {
                errorMessageTextView.text = "Minden mező kitöltése kötelező!"
                return@setOnClickListener
            }

            val email = "$username@example.com"

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("LOGIN_SUCCESS", "signInWithEmail:success")
                        Toast.makeText(baseContext, "Sikeres bejelentkezés.", Toast.LENGTH_SHORT).show()
                        errorMessageTextView.text = ""

                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        Log.w("LOGIN_FAILURE", "signInWithEmail:failure", task.exception)
                        errorMessageTextView.text = "Hibás felhasználónév vagy jelszó."
                        Toast.makeText(baseContext, "A bejelentkezés sikertelen.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // ======================================================
        // EZ AZ ÚJ KÓDRÉSZLET, A FÁJL VÉGÉN, DE MÉG AZ ONCREATE-EN BELÜL
        // ======================================================
        switchToRegisterTextView.setOnClickListener {
            // Létrehozunk egy "szándékot" (Intent), hogy elindítsuk a RegisterActivity-t
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        // ======================================================
    }
}