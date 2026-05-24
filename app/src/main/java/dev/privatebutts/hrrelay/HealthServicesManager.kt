package dev.privatebutts.hrrelay

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DeltaDataType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class MeasureMessage {
    data class MeasureAvailability(val availability: DataTypeAvailability) : MeasureMessage()
    data class MeasureData(val bpm: Double) : MeasureMessage()
    data class MeasureError(val error: Throwable) : MeasureMessage()
}

class HealthServicesManager(context: Context) {

    private val measureClient = HealthServices.getClient(context).measureClient

    fun heartRateMeasureFlow(): Flow<MeasureMessage> = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onRegistered() {
                Log.d(TAG, "Heart rate callback registered")
            }

            override fun onRegistrationFailed(exception: Throwable) {
                Log.e(TAG, "Heart rate registration failed", exception)
                trySendBlocking(MeasureMessage.MeasureError(exception))
            }

            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {
                if (availability is DataTypeAvailability) {
                    trySendBlocking(MeasureMessage.MeasureAvailability(availability))
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val bpm = data.getData(DataType.HEART_RATE_BPM)
                if (bpm.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    val value = (bpm.last() as? androidx.health.services.client.data.SampleDataPoint<Double>)?.value
                    if (value != null) {
                        trySendBlocking(MeasureMessage.MeasureData(value))
                    }
                }
            }
        }

        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)

        awaitClose {
            try {
                measureClient.unregisterMeasureCallbackAsync(
                    DataType.HEART_RATE_BPM, callback
                ).get()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val TAG = "HealthServicesManager"
    }
}
