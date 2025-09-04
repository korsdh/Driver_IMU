package com.example.driver_imu

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class MainActivity : AppCompatActivity(), SensorEventListener {
    private val TAG = "MainActivity"
    // 센서 관리를 위한 SensorManager와 Sensor 객체
    private var sensorManager: SensorManager? = null
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magneticSensor: Sensor? = null

    // UI 요소
    private var textViewX: TextView? = null
    private var textViewY: TextView? = null
    private var textViewZ: TextView? = null
    private var accTextViewX: TextView? = null
    private var accTextViewY: TextView? = null
    private var accTextViewZ: TextView? = null
    private var magTextViewX: TextView? = null
    private var magTextViewY: TextView? = null
    private var magTextViewZ: TextView? = null
    private var textViewTimestamp: TextView? = null
    private var startButton: Button? = null

    // 센서 값을 저장할 배열
    private val gyroscopeValues: FloatArray = FloatArray(3)
    private val accelerometerValues: FloatArray = FloatArray(3)
    private val magneticValues: FloatArray = FloatArray(3)

    private var isMeasuring: Boolean = false
    private var csvWriter: BufferedWriter? = null
    private var selectedCategory: String? = null
    private var selectedLabel: String? = null
    private var startTime: Long = 0L
    private var currentFile: File? = null


    // 주기적인 UI 업데이트를 위한 Handler
    private val handler: Handler = Handler()
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)

        // UI 요소 초기화
        textViewX = findViewById(R.id.textViewX)
        textViewY = findViewById(R.id.textViewY)
        textViewZ = findViewById(R.id.textViewZ)
        textViewTimestamp = findViewById(R.id.textViewTimestamp)
        accTextViewX = findViewById(R.id.textViewAccX)
        accTextViewY = findViewById(R.id.textViewAccY)
        accTextViewZ = findViewById(R.id.textViewAccZ)
        magTextViewX = findViewById(R.id.textViewMagX)
        magTextViewY = findViewById(R.id.textViewMagY)
        magTextViewZ = findViewById(R.id.textViewMagZ)
        startButton = findViewById(R.id.buttonStart)

        selectedLabel = intent.getStringExtra("selected_label")
        selectedCategory = intent.getStringExtra("selected_category")
        selectedLabel?.let {
            Toast.makeText(this, "선택된 레이블: $it", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Selected label: $it")
        }

        // 시스템 서비스로부터 SensorManager 인스턴스 가져오기
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        // 기본 자이로스코프 센서 가져오기
        gyroscopeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magneticSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // 기기에 자이로스코프 센서가 없는 경우 처리
        if (gyroscopeSensor == null) {
            Toast.makeText(this, "이 기기에는 자이로스코프 센서가 없습니다.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Gyroscope sensor not available")
            finish()
            return
        }
        if (accelerometerSensor == null) {
            Toast.makeText(this, "이 기기에는 가속도계 센서가 없습니다.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Accelerometer sensor not available")
        }
        if (magneticSensor == null) {
            Toast.makeText(this, "이 기기에는 지자기 센서가 없습니다.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Magnetic sensor not available")
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
//        sensorManager?.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        Log.d(TAG, "onResume")
        gyroscopeSensor?.also {
            sensorManager?.registerListener(this, it, SAMPLING_PERIOD_US)
        }
        accelerometerSensor?.also {
            sensorManager?.registerListener(this, it, SAMPLING_PERIOD_US)
        }
        magneticSensor?.also {
            sensorManager?.registerListener(this, it, SAMPLING_PERIOD_US)
        }
        // 주기적 업데이트 시작
        updateRunnable?.let { handler.post(it) }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
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
//                Log.d(TAG, "Gyroscope: ${event.values.joinToString()}")
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerValues, 0, 3)
//                Log.d(TAG, "Accelerometer: ${event.values.joinToString()}")
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magneticValues, 0, 3)
//                Log.d(TAG, "Magnetic: ${event.values.joinToString()}")
            }
        }

        if (isMeasuring) {
            val elapsedTime = System.currentTimeMillis() - startTime
            csvWriter?.write(
                "$elapsedTime,${gyroscopeValues[0]},${gyroscopeValues[1]},${gyroscopeValues[2]},${accelerometerValues[0]},${accelerometerValues[1]},${accelerometerValues[2]},${magneticValues[0]},${magneticValues[1]},${magneticValues[2]}\n"
            )
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

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
        magTextViewX?.text = String.format(Locale.getDefault(), "%.2f μT", magneticValues[0])
        magTextViewY?.text = String.format(Locale.getDefault(), "%.2f μT", magneticValues[1])
        magTextViewZ?.text = String.format(Locale.getDefault(), "%.2f μT", magneticValues[2])
        textViewTimestamp?.text = currentTimestamp
        Log.d(TAG, "UI updated")
    }

    private fun startMeasurement() {
        val category = selectedCategory ?: return
        val label = selectedLabel ?: return
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = File(getExternalFilesDir(null), "Driver/$category/$label")
        Log.d("File directory", "File Directory: $dir")
        Toast.makeText(this, "File Directory: $dir", Toast.LENGTH_SHORT).show()
        if (!dir.exists()) {
            dir.mkdirs()
        }
        currentFile = File(dir, "${label}_${timeStamp}.csv")
        csvWriter = BufferedWriter(FileWriter(currentFile))
        csvWriter?.write("timestamp,gyro_x,gyro_y,gyro_z,acc_x,acc_y,acc_z,mag_x,mag_y,mag_z\n")
        startTime = System.currentTimeMillis()
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
        val savedPath = currentFile?.absolutePath ?: "Unknown directory"
//        Toast.makeText(this ,"$savedPath \n 에 저장되었습니다!", Toast.LENGTH_SHORT).show()
        AlertDialog.Builder(this)
            .setTitle("저장 완료")
            .setMessage(savedPath)
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    companion object {
        // 데이터 취득 주기 (밀리초 단위, 여기서는 1초)
        private const val UPDATE_INTERVAL_MS: Int = 100
        // 마이크로초 단위 100Hz
        private const val SAMPLING_PERIOD_US: Int = 10000
    }
}