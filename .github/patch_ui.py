from pathlib import Path

p = Path('app/src/main/java/com/forcefocus/app/MainActivity.java')
s = p.read_text(encoding='utf-8')

old = '''        ((FrameLayout) runningOverlay).addView(runningOrb, roLp);\n    }\n\n    private void buildEdgeTrigger() {'''
new = '''        ((FrameLayout) runningOverlay).addView(runningOrb, roLp);\n\n        View runningWhitelist = weekendActionIcon(MiniGlyphView.WHITELIST, v -> openRunningWhitelist());\n        FrameLayout.LayoutParams rwLp = new FrameLayout.LayoutParams(dp(44), dp(44));\n        rwLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;\n        rwLp.bottomMargin = dp(58);\n        ((FrameLayout) runningOverlay).addView(runningWhitelist, rwLp);\n    }\n\n    private void openRunningWhitelist() {\n        LockState.Session session = LockState.current(this);\n        if (session == null) return;\n        sheetCard.removeAllViews();\n        LinearLayout row = new LinearLayout(this);\n        row.setOrientation(LinearLayout.HORIZONTAL);\n        row.setGravity(Gravity.CENTER);\n        addAppIconsToRow(row, session.mode, false);\n        sheetCard.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));\n        sheetOverlay.setVisibility(View.VISIBLE);\n    }\n\n    private void buildEdgeTrigger() {'''
if old not in s:
    raise SystemExit('running overlay patch anchor not found')
s = s.replace(old, new, 1)

s = s.replace('week.addView(tv, new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.WRAP_CONTENT));',
              'week.addView(tv, new LinearLayout.LayoutParams(dp(50), ViewGroup.LayoutParams.WRAP_CONTENT));', 1)
s = s.replace('int cellW = dp(48), cellH = dp(66);', 'int cellW = dp(46), cellH = dp(66);', 1)

# Ensure the two user-provided mode images are shown as images, not swallowed by crop behavior.
s = s.replace('iv.setScaleType(ImageView.ScaleType.CENTER_CROP);', 'iv.setScaleType(ImageView.ScaleType.FIT_CENTER);', 1)

p.write_text(s, encoding='utf-8')
