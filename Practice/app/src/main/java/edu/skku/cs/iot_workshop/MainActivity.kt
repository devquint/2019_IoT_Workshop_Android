package edu.skku.cs.iot_workshop

import android.content.Context
import android.graphics.Color.rgb
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random.Default.nextInt

class MainActivity : AppCompatActivity() {

    lateinit var sensorManager: SensorManager // 센서 매니저

    var mag = FloatArray(3) // 자력계
    var acc = FloatArray(3) // 가속계
    var gyro = FloatArray(3) // 자이로스코프(관성계)
    var lacc = FloatArray(3) // 선형가속계
    var grav = FloatArray(3) // 중력계

    val rotationMatrix = FloatArray(9) // 회전행렬 3x3

    override fun onCreate(savedInstanceState: Bundle?) { // 애플리케이션 실행
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        registerListener()
    }

    val chartLines = 6 // 차트 개수
    val dataSet = ArrayList<LineDataSet>()
    val entries = ArrayList<ArrayList<Entry>>()
    var chartInited: Boolean = false
    fun initChart() { // 차트 초기화
        if (!chartInited) {
            for (i in 0 until chartLines) {
                entries.add(ArrayList()) // 초기화
                dataSet.add(LineDataSet(entries[i], i.toString() + "th value")) // 차트 라벨 텍스트 변경
                dataSet[i].color = rgb(nextInt(255), nextInt(255), nextInt(255)) // 차트 선 색 변경(무작위)
                dataSet[i].setCircleColor(dataSet[i].color) // 차트 원 색 변경(선과 같도록)
                dataSet[i].lineWidth = 1.0f // 차트 선 두께 변경
                dataSet[i].circleRadius = 2.0f // 차트 원 반지름 변경
                dataSet[i].valueTextSize = 10.0f // 차트 값 텍스트 크기 변경
            }
            chart.setTouchEnabled(true) // 터치 허용
            chart.setDragEnabled(true) // 드래그 허용
            chart.setScaleEnabled(true) // 크기 변경 허용
            chart.setPinchZoom(true) // 핀치줌 허용
            chartInited = true
        }
    }

    fun addToChart(type: Int, xVal: Float, yVal: Float) { // 차트에 값 추가
        initChart()
        dataSet[type].addEntry(Entry(xVal, yVal))
        //entries[type].sortBy{it.x} // 무작위로 값을 넣을 때 x좌표 정렬 필요, 시간순이면 주석처리
        finalizeChart()
    }

    fun finalizeChart() { // 차트 갱신
        initChart()
        val lineData = LineData(dataSet.toList())
        chart.data = lineData
        chart.invalidate()
    }

    var lastTime: Long = -1L
    fun getTime(timeStamp: Long): Float { // 현재 시간을 적절히 Float로 변환 (ms단위, 처음 시작 후 경과 시간)
        val nowTime = (Date()).getTime() + (timeStamp - System.nanoTime()) / 1000000L;
        if (lastTime == -1L) lastTime = nowTime
        return (nowTime - lastTime).toFloat()
    }

    fun getRotationMatrix(): Boolean {
        if (acc.isEmpty() || mag.isEmpty()) return false;
        SensorManager.getRotationMatrix(rotationMatrix, null, acc, mag)
        return true;
    }

    fun gcsTransform(target: FloatArray): FloatArray {
        if (!getRotationMatrix()) return target
        val ret = FloatArray(3)
        for (i in 0 until 3) ret[i] = 0.0f
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                ret[i] += target[j] * rotationMatrix[i * 3 + j]
            }
        }
        for (i in 0 until 3) target[i] = ret[i]
        return ret
    }


    fun refresh(event: SensorEvent) { // 센서 변화 감지 시 호출
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                acc = event.values // 센서 값을 acc 배열에 저장
                for (i in 0 until 3) { // 0 ~ 2 반복
                    addToChart(
                        i,
                        getTime(event.timestamp),
                        acc[i]
                    ) // 차트에 값 추가, getTime(event.timestamp)로 시간
                }
            }
            Sensor.TYPE_GRAVITY -> {
                grav = event.values

            }
            Sensor.TYPE_GYROSCOPE -> {
                gyro = event.values // 센서 값을 gyro 배열에 저장
                /*
                val transformed = gcsTransform(gyro) // GCS로의 변환
                for (i in 0 until 3) { // 0 ~ 2 반복
                    addToChart(
                        i + 3, // 0 ~ 2는 acc에서 사용중, 3~5 사용
                        getTime(event.timestamp),
                        transformed[i] // 변환값으로 차트에 추가
                    ) // 차트에 값 추가, getTime(event.timestamp)로 시간
                }
                 */
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                lacc = event.values

            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mag = event.values

            }
        }
    }


    fun registerListener() { // 센서 리스너 등록
        val sensors = intArrayOf(
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_GRAVITY
        )

        for (type in sensors) {
            val mSensor: Sensor = sensorManager.getDefaultSensor(type)
            val sensorEventListener = MySensorEventListener()
            sensorManager.registerListener(sensorEventListener, mSensor, SENSOR_DELAY_NORMAL)
        }
    }

    inner class MySensorEventListener : SensorEventListener { // 센서 변화 감지
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
        override fun onSensorChanged(event: SensorEvent) {
            this@MainActivity.refresh(event)
        }
    }
}
