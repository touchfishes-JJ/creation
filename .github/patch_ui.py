from pathlib import Path
import zlib,base64,json,shutil
root=Path('.')
data=json.loads(zlib.decompress(base64.b64decode('eNrtfWl3HMd16F9pQZIxI/Q0ZsUygwEDgqCEZ2wPAKUwBA/cmGlg2pzN3Y3NEM6hk2iLJUuOJMuLHFmyHSuJTCl2YmuNznnvnzgESH7yX3j33qrqruplZgBCTl6OlZiY7qq6devWrbvVreqjIbPbHd3atZt1Y8cx601rqKwNdZu7O3bb1Y40u64N1zotw2zXnY5dN6B2066Znt1pD2vHG+2NNi/RjjbaGvzXNluW2zVrFmu33XFq1antan...TRUNCATED_FOR_BREVITY...')).decode('utf-8'))
java=root/'app/src/main/java/com/forcefocus/app'
if java.exists(): shutil.rmtree(java)
for rel,content in data.items():
 p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(content,encoding='utf-8')
print('ForceFocus v16 web UI + native features ready')
