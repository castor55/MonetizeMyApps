package net.monetizemyapp.android.data

data class BatteryInfo(val level: Float, val isCharging: Boolean) {
    override fun toString(): String {
        return "\n\t------\n\tBattery Level = $level\n\tIs Charging =$isCharging"
    }
}