package com.spendtrack.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendtrack.app.domain.model.TxnStatus

@Entity(tableName = "transactions", indices = [Index("timestamp"), Index("status")])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Money is always paise; never a floating-point rupee value. */
    val amountPaise: Long,
    val payee: String,
    val category: String? = null,
    val note: String? = null,
    /** Payment time, epoch millis. */
    val timestamp: Long,
    /** Source app package, or [MANUAL_PKG] for cash entries. */
    val appPkg: String,
    /** Original notification text, always kept. */
    val rawText: String,
    val status: TxnStatus = TxnStatus.PENDING,
) {
    companion object {
        const val MANUAL_PKG = "manual"
    }
}
