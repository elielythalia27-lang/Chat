package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.moodle.ChatSyncBundleJson
import com.example.data.moodle.MessagePayloadJson
import com.example.data.moodle.UcfMoodleClient
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Nexus", appName)
  }

  @Test
  fun `verify cloud 4MB limit and json sync`() {
    assertEquals(4 * 1024 * 1024L, UcfMoodleClient.MAX_FILE_SIZE_BYTES)

    val bundle = ChatSyncBundleJson(
      chatId = "group_team",
      title = "Equipo Nexus",
      type = "GROUP",
      messages = listOf(
        MessagePayloadJson(
          id = "msg_1",
          chatId = "group_team",
          senderId = "usuario1",
          senderName = "Alejandro",
          text = "Mensaje en la nube",
          attachmentSize = 2048L
        )
      )
    )

    val jsonObj = bundle.toJson()
    val reconstructed = ChatSyncBundleJson.fromJson(jsonObj)
    assertEquals("group_team", reconstructed.chatId)
    assertEquals(1, reconstructed.messages.size)
    assertEquals("Mensaje en la nube", reconstructed.messages[0].text)
  }
}
