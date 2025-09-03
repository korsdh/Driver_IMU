package com.example.driver_imu

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class MainActivity : AppCompatActivity(), SensorEventListener {
    // 센서 관리를 위한 SensorManager와 Sensor 객체
    private var sensorManager: SensorManager? = null
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    // UI 요소
    private var textViewX: TextView? = null
    private var textViewY: TextView? = null
    private var textViewZ: TextView? = null
    private var accTextViewX: TextView? = null
    private var accTextViewY: TextView? = null
    private var accTextViewZ: TextView? = null
    private var textViewTimestamp: TextView? = null
    private var startButton: Button? = null

    // 센서 값을 저장할 배열
    private val gyroscopeValues: FloatArray = FloatArray(3)
    private val accelerometerValues: FloatArray = FloatArray(3)

    private var isMeasuring: Boolean = false
    private var csvWriter: BufferedWriter? = null
    private var selectedCategory: String? = null
    private var selectedLabel: String? = null


    // 주기적인 UI 업데이트를 위한 Handler
    private val handler: Handler = Handler()
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI 요소 초기화
        textViewX = findViewById(R.id.textViewX)
        textViewY = findViewById(R.id.textViewY)
        textViewZ = findViewById(R.id.textViewZ)
        textViewTimestamp = findViewById(R.id.textViewTimestamp)
        accTextViewX = findViewById(R.id.textViewAccX)
        accTextViewY = findViewById(R.id.textViewAccY)
        accTextViewZ = findViewById(R.id.textViewAccZ)
        startButton = findViewById(R.id.buttonStart)

        selectedLabel = intent.getStringExtra("selected_label")
        selectedCategory = intent.getStringExtra("selected_category")
        selectedLabel?.let {
            Toast.makeText(this, "선택된 레이블: $it", Toast.LENGTH_SHORT).show()
        }

        // 시스템 서비스로부터 SensorManager 인스턴스 가져오기
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        // 기본 자이로스코프 센서 가져오기
        gyroscopeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // 기기에 자이로스코프 센서가 없는 경우 처리
        if (gyroscopeSensor == null) {
            Toast.makeText(this, "이 기기에는 자이로스코프 센서가 없습니다.", Toast.LENGTH_SHORT).show()
            finish() // 앱 종료
            return
        }
        if (accelerometerSensor == null) {
            Toast.makeText(this, "이 기기에는 가속도계 센서가 없습니다.", Toast.LENGTH_SHORT).show()
        }

        startButton?.setOnClickListener {
            if (!isMeasuring) {
                startMeasurement()
            } else {
                stopMeasurement()
            }
        }

        // 주기적으로 화면을 업데이트하는 Runnable 정의
        updateRunnable = object : Runnable {
            override fun run() {
                updateUI()
                // 지정된 시간(UPDATE_INTERVAL_MS) 후에 자기 자신을 다시 실행
                handler.postDelayed(this, UPDATE_INTERVAL_MS.toLong())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // SensorEventListener 등록
        // SENSOR_DELAY_NORMAL은 일반적인 UI 업데이트에 적합한 속도
        sensorManager?.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        accelerometerSensor?.also {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        // 주기적 업데이트 시작
        updateRunnable?.let { handler.post(it) }
    }

    override fun onPause() {
        super.onPause()
        // 배터리 절약을 위해 SensorEventListener 등록 해제
        sensorManager?.unregisterListener(this)
        // 주기적 업데이트 중지
        updateRunnable?.let { handler.removeCallbacks(it) }
        if(isMeasuring){
            stopMeasurement()
        }
    }

    /**
     * 센서 데이터가 변경될 때마다 호출되는 콜백 메서드
     */
    override fun onSensorChanged(event: SensorEvent) {
        // 이벤트가 자이로스코프 센서로부터 온 것인지 확인
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                System.arraycopy(event.values, 0, gyroscopeValues, 0, 3)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerValues, 0, 3)
            }
        }

        if (isMeasuring) {
            val timestamp = System.currentTimeMillis()
            csvWriter?.write(
                "$timestamp,${gyroscopeValues[0]},${gyroscopeValues[1]},${gyroscopeValues[2]},${accelerometerValues[0]},${accelerometerValues[1]},${accelerometerValues[2]}\n"
            )
        }
    }

    /**
     * 센서의 정확도가 변경될 때 호출되는 콜백 메서드
     * 이 예제에서는 사용하지 않음
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 필요 시 정확도 변경에 대한 로직 구현
    }

    /**
     * UI를 최신 센서 값으로 업데이트하는 메서드
     */
    private fun updateUI() {
        // 현재 시간을 보기 좋은 형식으로 포맷
        val sdf: SimpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTimestamp: String = sdf.format(Date())

        // 각 TextView에 센서 값과 타임스탬프 설정
        // 소수점 둘째 자리까지 표시
        textViewX?.text = String.format(Locale.getDefault(), "%.2f rad/s", gyroscopeValues[0])
        textViewY?.text = String.format(Locale.getDefault(), "%.2f rad/s", gyroscopeValues[1])
        textViewZ?.text = String.format(Locale.getDefault(), "%.2f rad/s", gyroscopeValues[2])
        accTextViewX?.text = String.format(Locale.getDefault(), "%.2f m/s²", accelerometerValues[0])
        accTextViewY?.text = String.format(Locale.getDefault(), "%.2f m/s²", accelerometerValues[1])
        accTextViewZ?.text = String.format(Locale.getDefault(), "%.2f m/s²", accelerometerValues[2])
        textViewTimestamp?.text = currentTimestamp
    }

    private fun startMeasurement() {
        val category = selectedCategory ?: return
        val label = selectedLabel ?: return
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = File(getExternalFilesDir(null), "Driver/$category/$label")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "${label}_${timeStamp}.csv")
        csvWriter = BufferedWriter(FileWriter(file))
        csvWriter?.write("timestamp,gyro_x,gyro_y,gyro_z,acc_x,acc_y,acc_z\n")
        isMeasuring = true
        startButton?.text = "측정종료"
        Toast.makeText(this, "측정을 시작합니다.", Toast.LENGTH_SHORT).show()
    }

    private fun stopMeasurement() {
        isMeasuring = false
        csvWriter?.flush()
        csvWriter?.close()
        startButton?.text = "측정시작"
        Toast.makeText(this, "측정을 종료했습니다.", Toast.LENGTH_SHORT).show()
    }

    companion object {
        // 데이터 취득 주기 (밀리초 단위, 여기서는 1초)
        private const val UPDATE_INTERVAL_MS: Int = 100
    }
}