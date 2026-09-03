(function () {
  "use strict";

  if (!window.NativeBridge || window.ForceFocusNative) return;

  const TASK_APPS = {
    "简历": ["WPS"],
    "岗位调研": ["小红书", "WPS"],
    "考公": ["粉笔"],
    "磨耳朵": ["录音机", "WPS"]
  };
  const $ = selector => document.querySelector(selector);
  const $$ = selector => Array.from(document.querySelectorAll(selector));

  let state = {};
  let selectedTask = "简历";
  let focusTimer = null;
  let settingWheels = false;
  let touchStartX = null;
  let touchStartY = null;

  function parseJson(text, fallback) {
    try {
      return JSON.parse(String(text));
    } catch (_) {
      return fallback;
    }
  }

  function loadState() {
    state = parseJson(NativeBridge.getStateJson(), {});
    selectedTask = TASK_APPS[state.selectedTask] ? state.selectedTask : "简历";
    return state;
  }

  function toast(text) {
    const element = $("#toast");
    if (!element) return;
    element.textContent = text;
    element.style.display = "block";
    clearTimeout(window.__forceFocusNativeToast);
    window.__forceFocusNativeToast = setTimeout(() => {
      element.style.display = "none";
    }, 1300);
  }

  function appGlyph(name) {
    if (name === "小红书") return "红";
    if (name === "录音机") return "录";
    if (name === "粉笔") return "粉";
    return "W";
  }

  function renderApps(selector, task) {
    const container = $(selector);
    if (!container) return;
    container.innerHTML = "";
    (TASK_APPS[task] || []).forEach(name => {
      const button = document.createElement("button");
      button.className = "app";
      button.textContent = appGlyph(name);
      button.title = name;
      button.dataset.nativeApp = name;
      container.appendChild(button);
    });
  }

  function showModal(html) {
    const panel = $("#modalPanel");
    const modal = $("#modal");
    if (!panel || !modal) return;
    panel.innerHTML = html;
    modal.classList.add("open");
  }

  function closeModal() {
    const modal = $("#modal");
    if (modal) modal.classList.remove("open");
  }

  function currentWheelValue(rootId, fallback) {
    const current = $(rootId + " .wheelItem.current");
    const parsed = current ? parseInt(current.textContent, 10) : NaN;
    return Number.isFinite(parsed) ? parsed : fallback;
  }

  function setWheel(rootId, target, modulus, fallback) {
    const root = $(rootId);
    if (!root) return;
    let current = currentWheelValue(rootId, fallback);
    target = ((target % modulus) + modulus) % modulus;
    const forward = (target - current + modulus) % modulus;
    const backward = (current - target + modulus) % modulus;
    const delta = forward <= backward ? 22 : -22;
    const count = Math.min(forward, backward);
    settingWheels = true;
    for (let i = 0; i < count; i += 1) {
      root.dispatchEvent(new WheelEvent("wheel", {
        deltaY: delta,
        bubbles: true,
        cancelable: true
      }));
    }
    settingWheels = false;
  }

  function setDuration(hours, minutes) {
    setWheel("#hourWheel", Number(hours) || 0, 5, 1);
    setWheel("#minuteWheel", Number(minutes) || 0, 60, 0);
  }

  function selectedDuration() {
    const hours = currentWheelValue("#hourWheel", state.lockedHours || 1);
    const minutes = currentWheelValue("#minuteWheel", state.lockedMinutes || 0);
    return {hours, minutes, seconds: Math.max(1, hours * 3600 + minutes * 60)};
  }

  function formatCountdown(totalSeconds) {
    const seconds = Math.max(0, Math.floor(totalSeconds));
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const remainder = seconds % 60;
    return [hours, minutes, remainder]
      .map(value => String(value).padStart(2, "0"))
      .join(":");
  }

  function updateEarlyButton(remaining) {
    const button = $("#endBtn");
    if (button) button.textContent = "提前结束（本周剩余 " + remaining + " 次）";
  }

  function stopFocusTimer() {
    if (focusTimer !== null) {
      clearInterval(focusTimer);
      focusTimer = null;
    }
  }

  function hideFocus() {
    stopFocusTimer();
    const overlay = $("#focusState");
    if (overlay) overlay.classList.remove("open");
  }

  function showFocus(endMillis, task, earlyRemaining) {
    stopFocusTimer();
    renderApps("#focusApps", task);
    updateEarlyButton(earlyRemaining);
    const overlay = $("#focusState");
    const countdown = $("#countdown");
    if (!overlay || !countdown) return;
    overlay.classList.add("open");

    const tick = () => {
      const remaining = Math.max(0, Math.ceil((Number(endMillis) - Date.now()) / 1000));
      countdown.textContent = formatCountdown(remaining);
      if (remaining <= 0) {
        stopFocusTimer();
        NativeBridge.finishFocus();
        state.active = false;
        overlay.classList.remove("open");
        toast("本次专注完成");
        setTimeout(syncCalendar, 0);
      }
    };
    tick();
    focusTimer = setInterval(tick, 1000);
  }

  function restoreWeekendTasks() {
    const tasks = Array.isArray(state.weekendTasks) ? state.weekendTasks : [];
    $$(".slot").forEach((slot, index) => {
      const task = TASK_APPS[tasks[index]] ? tasks[index] : ["简历", "岗位调研", "考公"][index];
      const current = slot.querySelector(".chosen");
      if (current && current.textContent !== task) {
        const option = Array.from(slot.querySelectorAll(".workDropdown button[data-task]"))
          .find(button => button.dataset.task === task);
        if (option) option.click();
      }
    });
    const toastElement = $("#toast");
    if (toastElement) toastElement.style.display = "none";
  }

  function applyModeRule() {
    const weekend = Boolean(state.weekendLocked || state.isWeekend);
    const button = weekend ? $("#forestBtn") : $("#treeBtn");
    if (button) button.click();
  }

  function applyState() {
    selectedTask = TASK_APPS[state.selectedTask] ? state.selectedTask : "简历";
    renderApps("#normalApps", selectedTask);

    if (state.durationLocked) {
      setDuration(state.lockedHours, state.lockedMinutes);
    }
    const durationLockButton = $("#durationLockBtn");
    if (durationLockButton) {
      durationLockButton.classList.toggle("on", Boolean(state.durationLocked));
    }

    const lockImage = $("#weekendLockImg");
    if (lockImage) lockImage.style.visibility = state.weekendLocked ? "hidden" : "visible";
    restoreWeekendTasks();
    applyModeRule();

    if (state.active && Number(state.focusEnd) > Date.now()) {
      showFocus(state.focusEnd, state.focusTask, state.earlyRemaining);
    } else {
      hideFocus();
      updateEarlyButton(state.earlyRemaining == null ? 2 : state.earlyRemaining);
    }
    setTimeout(syncCalendar, 0);
  }

  function refresh() {
    loadState();
    applyState();
  }

  function displayedMonth() {
    const text = $("#month") ? $("#month").textContent : "";
    const match = text.match(/(\d{4})年(\d{1,2})月/);
    return match ? {year: Number(match[1]), month: Number(match[2])} : null;
  }

  function syncCalendar() {
    const shown = displayedMonth();
    if (!shown) return;
    const data = parseJson(
      NativeBridge.getCalendarMonthJson(shown.year, shown.month),
      {minutes: {}, marks: []}
    );
    const marked = new Set((data.marks || []).map(Number));
    $$("#grid .day").forEach(dayElement => {
      const numberElement = dayElement.querySelector(".num");
      const box = dayElement.querySelector(".box");
      const fill = dayElement.querySelector(".fill");
      if (!numberElement || !box || !fill) return;
      const day = Number(numberElement.textContent);
      const minutes = Math.max(0, Number((data.minutes || {})[String(day)]) || 0);
      fill.style.height = Math.min(100, minutes / (8 * 60) * 100) + "%";
      box.classList.toggle("marked", marked.has(day));
    });
  }

  function blockEvent(event) {
    event.preventDefault();
    event.stopImmediatePropagation();
  }

  document.addEventListener("click", event => {
    const target = event.target instanceof Element ? event.target : null;
    if (!target) return;

    const taskButton = target.closest(".task[data-task]");
    if (taskButton) {
      blockEvent(event);
      selectedTask = TASK_APPS[taskButton.dataset.task] ? taskButton.dataset.task : "简历";
      state.selectedTask = selectedTask;
      NativeBridge.setSelectedTask(selectedTask);
      const tasks = $("#tasks");
      if (tasks) tasks.classList.remove("open");
      renderApps("#normalApps", selectedTask);
      toast("已选择：" + selectedTask);
      return;
    }

    if (target.closest("#treeBtn") && state.weekendLocked) {
      blockEvent(event);
      toast("周末模式已锁定");
      return;
    }

    if (target.closest("#startBtn")) {
      blockEvent(event);
      const duration = selectedDuration();
      const end = Number(NativeBridge.startFocus(selectedTask, duration.seconds));
      state.active = true;
      state.focusTask = selectedTask;
      state.focusEnd = end;
      state.earlyRemaining = Number(NativeBridge.getEarlyRemaining());
      showFocus(end, selectedTask, state.earlyRemaining);
      return;
    }

    if (target.closest("#endBtn")) {
      blockEvent(event);
      const remaining = Number(NativeBridge.endFocusEarly());
      if (remaining < 0) {
        toast("本周提前结束次数已用完");
      } else {
        state.active = false;
        state.earlyRemaining = remaining;
        hideFocus();
        updateEarlyButton(remaining);
        toast("已提前结束");
        setTimeout(syncCalendar, 0);
      }
      return;
    }

    if (target.closest("#memoryBtn")) {
      blockEvent(event);
      if (state.durationLocked) {
        toast("时长已锁定");
        return;
      }
      const recent = parseJson(NativeBridge.getRecentDurationsJson(), []);
      const content = recent.length
        ? recent.map(seconds => {
            const hours = Math.floor(seconds / 3600);
            const minutes = Math.floor((seconds % 3600) / 60);
            return '<button class="memNative" data-seconds="' + seconds + '">' +
              hours + " h  " + String(minutes).padStart(2, "0") + " min</button>";
          }).join("")
        : "<p>暂无已使用时长</p>";
      showModal('<button class="close">×</button><h3>最近三次时长</h3>' + content);
      return;
    }

    const memoryChoice = target.closest(".memNative[data-seconds]");
    if (memoryChoice) {
      blockEvent(event);
      const seconds = Number(memoryChoice.dataset.seconds);
      setDuration(Math.floor(seconds / 3600), Math.floor((seconds % 3600) / 60));
      closeModal();
      toast("已回填时长");
      return;
    }

    if (target.closest("#durationLockBtn")) {
      blockEvent(event);
      const duration = selectedDuration();
      state.durationLocked = !state.durationLocked;
      NativeBridge.setDurationLock(
        state.durationLocked, duration.hours, duration.minutes
      );
      const button = $("#durationLockBtn");
      if (button) button.classList.toggle("on", state.durationLocked);
      toast(state.durationLocked ? "时长已锁定" : "时长已解锁");
      return;
    }

    if (target.closest("#hourWheel, #minuteWheel") && state.durationLocked) {
      blockEvent(event);
      toast("时长已锁定");
      return;
    }

    if (target.closest("#weekendLockBtn")) {
      blockEvent(event);
      state.weekendLocked = !state.weekendLocked;
      NativeBridge.setWeekendLocked(state.weekendLocked);
      const image = $("#weekendLockImg");
      if (image) image.style.visibility = state.weekendLocked ? "hidden" : "visible";
      if (state.weekendLocked) {
        const forest = $("#forestBtn");
        if (forest) forest.click();
      }
      toast(state.weekendLocked ? "周末模式已锁定" : "恢复自动切换");
      return;
    }

    const weekendTask = target.closest(".workDropdown button[data-task]");
    if (weekendTask) {
      const slot = weekendTask.closest(".slot");
      const index = slot ? Number(slot.dataset.slot) : -1;
      if (index >= 0 && index < 3) {
        if (!Array.isArray(state.weekendTasks)) state.weekendTasks = ["简历", "岗位调研", "考公"];
        state.weekendTasks[index] = weekendTask.dataset.task;
        NativeBridge.setWeekendTask(index, weekendTask.dataset.task);
      }
      return;
    }

    if (target.closest("#sidebarWhitelistBtn")) {
      blockEvent(event);
      const drawer = $("#drawer");
      if (drawer) drawer.classList.remove("open");
      const buttons = (TASK_APPS[selectedTask] || []).map(name =>
        '<button class="nativeWhitelistApp" data-app="' + name + '">' + name + "</button>"
      ).join("");
      showModal('<button class="close">×</button><h3>白名单</h3>' + buttons);
      return;
    }

    if (target.closest("#a11y")) {
      blockEvent(event);
      NativeBridge.openAccessibilitySettings();
      return;
    }

    if (target.closest("#alarm")) {
      blockEvent(event);
      NativeBridge.openExactAlarmSettings();
      return;
    }

    const appButton = target.closest(
      ".app[data-native-app], .nativeWhitelistApp[data-app], .whiteDropdown button[data-app]"
    );
    if (appButton) {
      blockEvent(event);
      const app = appButton.dataset.nativeApp || appButton.dataset.app || appButton.title;
      const dropdown = appButton.closest(".whiteDropdown");
      if (dropdown) dropdown.classList.remove("open");
      NativeBridge.launchApp(app);
      return;
    }

    if (target.closest("#prev, #next")) {
      setTimeout(syncCalendar, 0);
      return;
    }

    const day = target.closest("#grid .day");
    if (day) {
      const shown = displayedMonth();
      const numberElement = day.querySelector(".num");
      const box = day.querySelector(".box");
      if (shown && numberElement && box) {
        NativeBridge.setCalendarMark(
          shown.year,
          shown.month,
          Number(numberElement.textContent),
          !box.classList.contains("marked")
        );
      }
      return;
    }

    if (target.closest(".close")) {
      blockEvent(event);
      closeModal();
    }
  }, true);

  function blockLockedWheel(event) {
    if (settingWheels || !state.durationLocked) return;
    const target = event.target instanceof Element ? event.target : null;
    if (target && target.closest("#hourWheel, #minuteWheel")) {
      blockEvent(event);
      toast("时长已锁定");
    }
  }

  ["wheel", "touchstart", "pointerdown"].forEach(type => {
    document.addEventListener(type, blockLockedWheel, {capture: true, passive: false});
  });

  document.addEventListener("touchstart", event => {
    if (event.touches.length !== 1) return;
    touchStartX = event.touches[0].clientX;
    touchStartY = event.touches[0].clientY;
  }, {capture: true, passive: true});

  document.addEventListener("touchend", event => {
    if (touchStartX === null || !event.changedTouches.length) return;
    const dx = event.changedTouches[0].clientX - touchStartX;
    const dy = event.changedTouches[0].clientY - touchStartY;
    const drawer = $("#drawer");
    if (drawer && Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.4) {
      if (touchStartX <= 28 && dx > 0) drawer.classList.add("open");
      else if (drawer.classList.contains("open") && dx < 0) drawer.classList.remove("open");
    }
    touchStartX = null;
    touchStartY = null;
  }, {capture: true, passive: true});

  function handleBack() {
    const modal = $("#modal");
    if (modal && modal.classList.contains("open")) {
      modal.classList.remove("open");
      return true;
    }
    const drawer = $("#drawer");
    if (drawer && drawer.classList.contains("open")) {
      drawer.classList.remove("open");
      return true;
    }
    if (state.active) return true;
    const pages = $("#pages");
    if (pages && pages.style.transform === "translateX(-50%)") {
      pages.style.transform = "translateX(0)";
      return true;
    }
    return false;
  }

  window.ForceFocusNative = {refresh, handleBack, syncCalendar};
  loadState();
  applyState();
})();
