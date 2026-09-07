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
  fun testSentenceSplittingWithCommaShortSentenceNotSplit() {
    // 长度 <= 30 字符：保持整句中间有标点也不拆
    val text = "每一个清晨，都是新生活的开始。坚持热爱，奔赴山海！"
    val sentences = TextSegmenter.splitIntoSentences(text, splitComma = true)
    assertEquals(2, sentences.size)
    assertEquals("每一个清晨，都是新生活的开始。", sentences[0])
    assertEquals("坚持热爱，奔赴山海！", sentences[1])
  }

  @Test
  fun testSecondarySplitBetween31And80Chars() {
    // 31 ~ 80 字符：最靠近中间的标点拆为 2 段
    val sentence = "在浩瀚无垠的宇宙星空中每一个恒星，都有属于它独特的生命轨迹与演变过程。"
    assertTrue(sentence.length in 31..80)
    val sentences = TextSegmenter.splitIntoSentences(sentence, splitComma = true)
    assertEquals(2, sentences.size)
    assertEquals("在浩瀚无垠的宇宙星空中每一个恒星，", sentences[0])
    assertEquals("都有属于它独特的生命轨迹与演变过程。", sentences[1])
  }
}

