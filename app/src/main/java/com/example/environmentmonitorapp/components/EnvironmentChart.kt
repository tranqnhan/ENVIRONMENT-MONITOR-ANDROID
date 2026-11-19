package com.example.environmentmonitorapp.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.environmentmonitorapp.api.EnvironmentDataPoint
import com.patrykandpatrick.vico.compose.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import java.util.Date
import kotlin.math.roundToInt


@Composable
fun EnvironmentChart(
    environmentDataPoints: List<EnvironmentDataPoint>,
    dataType: EnvironmentDataType
) {
    val context = LocalContext.current

    if (environmentDataPoints.isEmpty()) {
        Text("No data available.")
        return
    }


    val modelProducer = remember { CartesianChartModelProducer() }

    val axisFormatter = remember(context) {
        CartesianValueFormatter { _, value, _ ->
            // Assumes your x-values are epoch seconds.
            val date = Date((value * 1_000).toLong())

            val dateText = DateFormat.getDateFormat(context).format(date)
            val timeText = DateFormat.getTimeFormat(context).format(date)

            "$dateText\n$timeText"
        }
    }

    LaunchedEffect(environmentDataPoints) {
        modelProducer.runTransaction {
            lineModel {
                series(
                    x = environmentDataPoints.map {
                        it.measuredAt.epochSecond.toDouble() +
                                it.measuredAt.nano / 1_000_000_000.0
                    },
                    y = environmentDataPoints.map {
                        when (dataType) {
                            EnvironmentDataType.TEMPERATURE -> it.temperature
                            EnvironmentDataType.CO2 -> it.co2Level
                            EnvironmentDataType.HUMIDITY -> it.humidity
                        }
                    }
                )
            }
        }
    }

    val rangeProvider = remember {
        object : CartesianLayerRangeProvider {
            override fun getMinY(
                minY: Double,
                maxY: Double,
                extraStore: ExtraStore
            ): Double {
                val padding = maxOf((maxY - minY) * 0.1, 0.5)
                return minY - padding
            }

            override fun getMaxY(
                minY: Double,
                maxY: Double,
                extraStore: ExtraStore
            ): Double {
                val padding = maxOf((maxY - minY) * 0.1, 0.5)
                return maxY + padding
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(
                            Fill(EnvironmentDataColor[dataType]!!)
                        ),
                        pointProvider = LineCartesianLayer.PointProvider.single(
                            LineCartesianLayer.Point(
                                rememberShapeComponent(
                                    fill = Fill(EnvironmentDataColor[dataType]!!),
                                    shape = CircleShape
                                )
                            )
                        ),
                    )
                ),

                rangeProvider = rangeProvider
            ),
            startAxis =
                if (dataType == EnvironmentDataType.CO2) {
                    VerticalAxis.rememberStart(
                        valueFormatter = CartesianValueFormatter { _, value, _ ->
                            value.roundToInt().toString()
                        }
                    )
                } else {
                    VerticalAxis.rememberStart()
                },
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = axisFormatter,
                label = rememberAxisLabelComponent(
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp
                    ),
                    lineCount = 2,
                    overflow = TextOverflow.Ellipsis
                ),
                itemPlacer = remember {
                    HorizontalAxis.ItemPlacer.aligned(
                        spacing = { 50 }
                    )
                }
            )
        ),
        scrollState = rememberVicoScrollState(
            initialScroll = Scroll.Absolute.Start,
            autoScroll = Scroll.Absolute.End,
            autoScrollCondition = AutoScrollCondition.OnModelGrowth
        ),
        zoomState = rememberVicoZoomState(
            initialZoom = Zoom.Content
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    )
}