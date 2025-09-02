package com.example.driver_imu

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {
    // 센서 관리를 위한 SensorManager와 Sensor 객체
    private var sensorManager: SensorManager? = null
    private var gyroscopeSensor: Sensor? = null

    // UI 요소
    private var textViewX: TextView? = null
    private var textViewY: TextView? = null
    private var textViewZ: TextView? = null
    private var textViewTimestamp: TextView? = null

    // 센서 값을 저장할 배열
    private val gyroscopeValues: FloatArray = FloatArray(3)

    // 주기적인 UI 업데이트를 위한 Handler
    private val handler: Handler = Handler()
    private var updateRunnable: Runnable? = null

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI 요소 초기화
        textViewX = findViewById(R.id.textViewX)
        textViewY = findViewById(R.id.textViewY)
        textViewZ = findViewById(R.id.textViewZ)
        textViewTimestamp = findViewById(R.id.textViewTimestamp)

        // 시스템 서비스로부터 SensorManager 인스턴스 가져오기
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        // 기본 자이로스코프 센서 가져오기
        gyroscopeSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // 기기에 자이로스코프 센서가 없는 경우 처리
        if (gyroscopeSensor == null) {
            Toast.makeText(this, "이 기기에는 자이로스코프 센서가 없습니다.", Toast.LENGTH_SHORT).show()
            finish() // 앱 종료
            return
        }

        // 주기적으로 화면을 업데이트하는 Runnable 정의
        updateRunnable = object : Runnable {
            @Override
            override fun run() {
                updateUI()
                // 지정된 시간(UPDATE_INTERVAL_MS) 후에 자기 자신을 다시 실행
                handler.postDelayed(this, UPDATE_INTERVAL_MS.toLong())
            }
        }
    }

    @Override
    override fun onResume() {
        super.onResume()
        // SensorEventListener 등록
        // SENSOR_DELAY_NORMAL은 일반적인 UI 업데이트에 적합한 속도
        sensorManager?.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
        // 주기적 업데이트 시작
        updateRunnable?.let { handler.post(it) }
    }

    @Override
    override fun onPause() {
        super.onPause()
        // 배터리 절약을 위해 SensorEventListener 등록 해제
        sensorManager?.unregisterListener(this)
        // 주기적 업데이트 중지
        updateRunnable?.let { handler.removeCallbacks(it) }
    }

    /**
     * 센서 데이터가 변경될 때마다 호출되는 콜백 메서드
     */
    @Override
    override fun onSensorChanged(event: SensorEvent) {
        // 이벤트가 자이로스코프 센서로부터 온 것인지 확인
        if (event.sensor.getType() === Sensor.TYPE_GYROSCOPE) {
            // 센서 값을 gyroscopeValues 배열에 복사
            // event.values[0]: x축 각속도 (rad/s)
            // event.values[1]: y축 각속도 (rad/s)
            // event.values[2]: z축 각속도 (rad/s)
            System.arraycopy(event.values, 0, gyroscopeValues, 0, 3)
        }
    }

    /**
     * 센서의 정확도가 변경될 때 호출되는 콜백 메서드
     * 이 예제에서는 사용하지 않음
     */
    @Override
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
        textViewX?.setText(String.format(Locale.getDefault(), "%.2f rad/s", gyroscopeValues.get(0)))
        textViewY?.setText(String.format(Locale.getDefault(), "%.2f rad/s", gyroscopeValues.get(1)))
        textViewZ?.setText(String.format(Locale.getDefault(), "%.2f rad/s", gyroscopeValues.get(2)))
        textViewTimestamp?.setText(currentTimestamp)
    }

    companion object {
        // 데이터 취득 주기 (밀리초 단위, 여기서는 1초)
        private const val UPDATE_INTERVAL_MS: Int = 1000
    }
}