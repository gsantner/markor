/***
  Copyright (c) 2010 CommonsWare, LLC
  Portions (c) somebody else who didn't bother to indicate who they were
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

#include "com_commonsware_cwac_anddown_AndDown.h"
#include "markdown.h"
#include "html.h"
#include "buffer.h"

#define INPUT_UNIT 64
#define OUTPUT_UNIT 64

JNIEXPORT jstring JNICALL Java_com_commonsware_cwac_anddown_AndDown_markdownToHtml
  (JNIEnv *env, jobject o, jstring raw) {
  struct hoedown_buffer *ib, *ob;
  jstring result;
  hoedown_renderer *renderer;
  hoedown_markdown *markdown;
  const char* str;

  str = (*env)->GetStringUTFChars(env, raw, NULL);

  ib = hoedown_buffer_new(INPUT_UNIT);
  hoedown_buffer_puts(ib, str);
  ob = hoedown_buffer_new(OUTPUT_UNIT);

  (*env)->ReleaseStringUTFChars(env, raw, str);

  renderer = hoedown_html_renderer_new(0, 0);
  markdown = hoedown_markdown_new(0, 16, renderer);

  hoedown_markdown_render(ob, ib->data, ib->size, markdown);
  hoedown_markdown_free(markdown);

  result=(*env)->NewStringUTF(env, hoedown_buffer_cstr(ob));

  /* cleanup */
  hoedown_buffer_free(ib);
  hoedown_buffer_free(ob);

  return(result);
}

