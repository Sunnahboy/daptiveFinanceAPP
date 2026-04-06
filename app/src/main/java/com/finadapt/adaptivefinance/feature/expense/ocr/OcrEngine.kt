package com.finadapt.adaptivefinance.feature.expense.ocr
import com.google.mlkit.vision.text.Text
import kotlin.math.atan2

//1. domain models no nulls allowed
data class OcrElement(
    val text: String,
    val centerX: Float,
    val centerY: Float,
    val height: Float,
    val angle: Double, //for skew correction
)
object OcrEngine {
    /**
     * Converts raw ML Kit Text into our strict domain model.
     * Enforces invariants immediately: Drops anything without valid geometry.
     */

    fun extractValidElements(visionText: Text):List<OcrElement>{
        val rawElements = visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }

        return rawElements.mapNotNull { element ->
            val box = element.boundingBox
            val corners = element.cornerPoints

            // invariant : Must have bounding box and corner points to exist in our system
            if (box == null || corners == null || corners.size < 4) return@mapNotNull null

            // Calculate real center
            val cX = box.exactCenterX()
            val cY = box.exactCenterY()
            val h = box.height().toFloat()


            //Calculate rotation angle using bottom-left and bottom-right corners
            val dx = (corners[2].x - corners[3].x).toDouble()
            val dy = (corners[2].y - corners[3].y).toDouble()
            val angle = atan2(dy, dx)


            OcrElement(element.text, cX, cY, h, angle)

        }

    }

}