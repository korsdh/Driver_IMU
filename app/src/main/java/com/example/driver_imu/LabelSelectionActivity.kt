package com.example.driver_imu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LabelSelectionActivity : AppCompatActivity(){
    private lateinit var categoryGroup: RadioGroup
    private lateinit var labelGroup: RadioGroup
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label_selection)

        categoryGroup = findViewById(R.id.categoryGroup)
        labelGroup = findViewById(R.id.labelGroup)
        startButton = findViewById(R.id.startButton)

        categoryGroup.setOnCheckedChangeListener{ _, checkedId ->
            labelGroup.removeAllViews()
            val labels = if (checkedId == R.id.radioAbnormal) {
                arrayOf("급가속", "급정지", "급좌회전", "급우회전")
            } else {
                arrayOf("정상 가속", "정상 감속", "정상 좌회전", "정상 우회전")
            }
            labels.forEach { text ->
                val rb = RadioButton(this)
                rb.text = text
                labelGroup.addView(rb)
            }
        }

        startButton.setOnClickListener {
            if(categoryGroup.checkedRadioButtonId == -1 || labelGroup.checkedRadioButtonId == -1){
                Toast.makeText(this, "레이블을 선택하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selected = findViewById<RadioButton>(labelGroup.checkedRadioButtonId)
            val intent = Intent(this, MainActivity::class.java)
            val category = if (categoryGroup.checkedRadioButtonId == R.id.radioAbnormal) {
                "이상"
            } else {
                "정상"
            }
            intent.putExtra("selected_category", category)
            startActivity(intent)
            intent.putExtra("selected_label", selected.text.toString())
            startActivity(intent)
        }
    }
}