#!/bin/bash
echo '<!DOCTYPE html>'
echo '<html><body><script type="text/javascript">'

npx terser build/classes/kotlin/js/main/lib/kotlin.js
npx terser build/classes/kotlin/js/main/kotlin-tetris.js

echo '</script></body></html>'
