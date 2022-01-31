package com.example.eyephone

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions

class EyeImageAnalyzer : ImageAnalysis.Analyzer {
    //declaring objects for image labeling
    //builds the model file
    val localModel = LocalModel.Builder()
        .setAssetFilePath("eyemodel.tflite")
        .build()
    //builds ImageLabelerOptions object for configuration for labeling the image, showing only labels with minimum 50% confidence level and 5 max displayed labels
    val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
        .setConfidenceThreshold(0.5f)
        .setMaxResultCount(5)
        .build()
    //build ImageLabeler object with previous configurations
    private val labeler = ImageLabeling.getClient(customImageLabelerOptions)
    //declare class variable to be accessed by CameraActivity.kt
    var listOfLabels : List<ImageLabel> = listOf()
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    for (label in labels) {
                        val text = label.text
                        val confidence = label.confidence
                        val index = label.index
                        println("text: $text")
                        println("confidence: $confidence")
                        println("index: $index")
                    }
                    addTextToSavePreview(labels)
                    println("Image processing was successful!")
                }
                .addOnFailureListener { e ->
                    println("Image processing failed!")
                }
        }
    }

    fun addTextToSavePreview(labels: List<ImageLabel>) {
        listOfLabels = labels
    }
}