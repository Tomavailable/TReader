package com.example

import com.example.data.TextSegmenter
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testSentenceSplittingWithoutComma() {
    val text = "这是第一句。这是第二句？这是第三句！"
    val sentences = TextSegmenter.splitIntoSentences(text, splitComma = false)
    assertEquals(3, sentences.size)
    assertEquals("这是第一句。", sentences[0])
    assertEquals("这是第二句？", sentences[1])
    assertEquals("这是第三句！", sentences[2])
  }

  @Test
  fun testSentenceSplittingWithComma() {
    val text = "每一个清晨，都是新生活的开始。坚持热爱，奔赴山海！"
    val sentences = TextSegmenter.splitIntoSentences(text, splitComma = true)
    assertEquals(4, sentences.size)
    assertEquals("每一个清晨，", sentences[0])
    assertEquals("都是新生活的开始。", sentences[1])
    assertEquals("坚持热爱，", sentences[2])
    assertEquals("奔赴山海！", sentences[3])
  }
}

