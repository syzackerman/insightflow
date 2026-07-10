const API_BASE =
  new URLSearchParams(window.location.search).get("api") ||
  window.INSIGHTFLOW_API_BASE ||
  (window.location.port === "8000" ? "http://localhost:8080" : window.location.origin);
const AUTH_TOKEN_KEY = "insightflow.jwt";

const currency = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0,
});

const number = new Intl.NumberFormat("en-US");

const shortCurrency = new Intl.NumberFormat("en-US", {
  notation: "compact",
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 1,
});

const percent = new Intl.NumberFormat("en-US", {
  maximumFractionDigits: 1,
  minimumFractionDigits: 0,
});

const chartColors = [
  "#237a72",
  "#385f8a",
  "#b66d2f",
  "#7d5ba6",
  "#a64f5f",
  "#4f7d45",
  "#a8872c",
  "#8a5d4b",
  "#4f7188",
  "#667085",
];

const icons = {
  revenue:
    '<path d="M4 19V5"/><path d="M4 19h16"/><path d="M8 15l3-4 3 2 5-7"/><path d="M17 6h2v2"/>',
  orders:
    '<path d="M7 3h10l3 4v14H4V3h3z"/><path d="M14 3v5h6"/><path d="M8 12h8"/><path d="M8 16h6"/>',
  aov:
    '<path d="M12 2v20"/><path d="M17 6.5c-.8-1-2.2-1.5-4-1.5-2.4 0-4 1.1-4 2.8 0 4 8 1.8 8 6.1 0 1.8-1.8 3.1-4.4 3.1-2 0-3.6-.6-4.6-1.8"/>',
  repeat:
    '<path d="M17 2l4 4-4 4"/><path d="M3 11V9a3 3 0 0 1 3-3h15"/><path d="M7 22l-4-4 4-4"/><path d="M21 13v2a3 3 0 0 1-3 3H3"/>',
  review:
    '<path d="M12 3l2.7 5.5 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.8 1-6.1-4.4-4.3 6.1-.9L12 3z"/>',
  delay:
    '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/><path d="M18.5 18.5l2.5 2.5"/>',
  download:
    '<path d="M12 3v12"/><path d="M7 10l5 5 5-5"/><path d="M5 21h14"/>',
  filter:
    '<path d="M4 5h16"/><path d="M7 12h10"/><path d="M10 19h4"/>',
  rotate:
    '<path d="M4 12a8 8 0 0 1 13.7-5.7L20 8"/><path d="M20 4v4h-4"/><path d="M20 12a8 8 0 0 1-13.7 5.7L4 16"/><path d="M4 20v-4h4"/>',
  logout:
    '<path d="M10 17l5-5-5-5"/><path d="M15 12H3"/><path d="M21 3v18h-7"/>',
  login:
    '<path d="M14 7l5 5-5 5"/><path d="M19 12H7"/><path d="M3 3v18h7"/>',
  "user-plus":
    '<circle cx="9" cy="8" r="4"/><path d="M3 21a6 6 0 0 1 12 0"/><path d="M19 8v6"/><path d="M16 11h6"/>',
  save:
    '<path d="M5 3h12l2 2v16H5z"/><path d="M8 3v6h8"/><path d="M8 21v-7h8v7"/>',
  "folder-open":
    '<path d="M3 7h7l2 3h9v8a3 3 0 0 1-3 3H5a2 2 0 0 1-2-2z"/><path d="M3 19l3-7h16l-3 7"/>',
  trash:
    '<path d="M4 7h16"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M6 7l1 14h10l1-14"/><path d="M9 7V4h6v3"/>',
  settings:
    '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2 3-.2-.1a1.7 1.7 0 0 0-2 .1 1.7 1.7 0 0 0-.8 1.7V22H9.2v-.3A1.7 1.7 0 0 0 8.4 20a1.7 1.7 0 0 0-2-.1l-.2.1-2-3 .1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3v-4h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1 2-3 .2.1a1.7 1.7 0 0 0 2-.1 1.7 1.7 0 0 0 .8-1.7V2h5.6v.3A1.7 1.7 0 0 0 15.6 4a1.7 1.7 0 0 0 2 .1l.2-.1 2 3-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.5 1h.1v4h-.1a1.7 1.7 0 0 0-1.5 1z"/>',
  sparkles:
    '<path d="M12 3l1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5z"/><path d="M19 14l.9 2.6 2.6.9-2.6.9L19 21l-.9-2.6-2.6-.9 2.6-.9z"/><path d="M5 15l.7 2 2 .7-2 .7-.7 2-.7-2-2-.7 2-.7z"/>',
  send:
    '<path d="M22 2L11 13"/><path d="M22 2l-7 20-4-9-9-4z"/>',
  "trend-down":
    '<path d="M4 6h16"/><path d="M4 18l5-5 4 4 7-8"/><path d="M15 9h5v5"/>',
  megaphone:
    '<path d="M3 11v2a2 2 0 0 0 2 2h2l4 5v-5l8 3V6l-8 3H5a2 2 0 0 0-2 2z"/><path d="M19 8a4 4 0 0 1 0 8"/>',
  calendar:
    '<path d="M7 3v4"/><path d="M17 3v4"/><path d="M4 8h16"/><path d="M5 5h14v16H5z"/><path d="M8 12h3"/><path d="M13 12h3"/><path d="M8 16h3"/>',
  map:
    '<path d="M9 18l-6 3V6l6-3 6 3 6-3v15l-6 3z"/><path d="M9 3v15"/><path d="M15 6v15"/>',
};

const state = {
  latestSummary: null,
  filtersLoaded: false,
  token: localStorage.getItem(AUTH_TOKEN_KEY),
  user: null,
  savedDashboards: [],
  preferences: null,
};

const kpiConfig = [
  ["totalRevenue", "Total Revenue", (value) => shortCurrency.format(asNumber(value)), "Delivered item sales", "revenue"],
  ["totalOrders", "Total Orders", (value) => number.format(asNumber(value)), "Distinct delivered orders", "orders"],
  ["averageOrderValue", "Average Order Value", (value) => currency.format(asNumber(value)), "Revenue per order", "aov"],
  ["repeatCustomerRate", "Repeat Customer Rate", (value) => `${percent.format(asNumber(value))}%`, "Customers with 2+ orders", "repeat"],
  ["averageReviewScore", "Avg Review Score", (value) => (value == null ? "N/A" : `${asNumber(value).toFixed(2)} / 5`), "Order-level average", "review"],
  ["deliveryDelayRate", "Delivery Delay Rate", (value) => `${percent.format(asNumber(value))}%`, "Delivered after estimate", "delay"],
];

const elements = {
  form: document.querySelector("#filterForm"),
  reset: document.querySelector("#resetFilters"),
  authForm: document.querySelector("#authForm"),
  displayName: document.querySelector("#displayName"),
  authEmail: document.querySelector("#authEmail"),
  authPassword: document.querySelector("#authPassword"),
  loginUser: document.querySelector("#loginUser"),
  registerUser: document.querySelector("#registerUser"),
  logoutUser: document.querySelector("#logoutUser"),
  accountStatus: document.querySelector("#accountStatus"),
  dashboardName: document.querySelector("#dashboardName"),
  saveDashboard: document.querySelector("#saveDashboard"),
  savedDashboards: document.querySelector("#savedDashboards"),
  loadDashboardView: document.querySelector("#loadDashboardView"),
  deleteDashboardView: document.querySelector("#deleteDashboardView"),
  preferenceTheme: document.querySelector("#preferenceTheme"),
  compactView: document.querySelector("#compactView"),
  savePreferences: document.querySelector("#savePreferences"),
  exportCsv: document.querySelector("#exportCsv"),
  generateReport: document.querySelector("#generateReport"),
  questionForm: document.querySelector("#questionForm"),
  analystQuestion: document.querySelector("#analystQuestion"),
  promptChips: document.querySelectorAll(".prompt-chip"),
  askAnalyst: document.querySelector("#askAnalyst"),
  reportMode: document.querySelector("#reportMode"),
  reportOutput: document.querySelector("#reportOutput"),
  queryMode: document.querySelector("#queryMode"),
  queryOutput: document.querySelector("#queryOutput"),
  message: document.querySelector("#messagePanel"),
  generatedAt: document.querySelector("#generatedAt"),
  sourceMode: document.querySelector("#sourceMode"),
  kpiGrid: document.querySelector("#kpiGrid"),
  monthlyChart: document.querySelector("#monthlyChart"),
  trendNote: document.querySelector("#trendNote"),
  categoryList: document.querySelector("#categoryList"),
  stateList: document.querySelector("#stateList"),
  reviewChart: document.querySelector("#reviewChart"),
  paymentList: document.querySelector("#paymentList"),
};

elements.form.addEventListener("submit", (event) => {
  event.preventDefault();
  loadDashboard();
});

elements.reset.addEventListener("click", () => {
  elements.form.reset();
  loadDashboard();
});

elements.exportCsv.addEventListener("click", exportCurrentCsv);
elements.generateReport.addEventListener("click", generateExecutiveReport);
elements.questionForm.addEventListener("submit", askBusinessAnalyst);
elements.promptChips.forEach((button) => {
  button.addEventListener("click", () => askSuggestedQuestion(button.dataset.prompt || ""));
});
elements.loginUser.addEventListener("click", () => authenticate("login"));
elements.registerUser.addEventListener("click", () => authenticate("register"));
elements.logoutUser.addEventListener("click", logout);
elements.saveDashboard.addEventListener("click", saveCurrentDashboard);
elements.loadDashboardView.addEventListener("click", loadSavedDashboardView);
elements.deleteDashboardView.addEventListener("click", deleteSavedDashboardView);
elements.savePreferences.addEventListener("click", saveDashboardPreferences);

enhanceButtonIcons();

function buildQuery() {
  const params = new URLSearchParams();
  for (const [key, value] of new FormData(elements.form).entries()) {
    if (String(value).trim()) {
      params.set(key, String(value).trim());
    }
  }
  return params.toString();
}

async function loadSummary() {
  const query = buildQuery();
  const url = `${API_BASE}/api/analytics/summary${query ? `?${query}` : ""}`;
  const response = await fetch(url);
  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(payload?.message || `Analytics API returned ${response.status}`);
  }
  return payload;
}

async function apiRequest(path, options = {}) {
  const headers = {
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...(options.headers || {}),
  };
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(payload?.message || `API returned ${response.status}`);
  }
  return payload;
}

async function postAi(path, body) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(payload?.message || `AI endpoint returned ${response.status}`);
  }
  return payload;
}

async function restoreSession() {
  if (!state.token) {
    renderSignedOutWorkspace();
    return;
  }
  try {
    state.user = await apiRequest("/api/auth/me");
    renderSignedInWorkspace();
    await loadUserWorkspace();
  } catch (error) {
    logout();
  }
}

async function authenticate(mode) {
  const email = elements.authEmail.value.trim();
  const password = elements.authPassword.value;
  const displayName = elements.displayName.value.trim();
  const path = mode === "register" ? "/api/auth/register" : "/api/auth/login";
  const body =
      mode === "register"
        ? { email, password, displayName }
        : { email, password };

  setAccountStatus(mode === "register" ? "Creating account..." : "Signing in...");
  try {
    const response = await apiRequest(path, {
      method: "POST",
      body: JSON.stringify(body),
    });
    state.token = response.token;
    state.user = response.user;
    localStorage.setItem(AUTH_TOKEN_KEY, response.token);
    elements.authPassword.value = "";
    renderSignedInWorkspace();
    await loadUserWorkspace();
  } catch (error) {
    setAccountStatus(error.message, true);
  }
}

function logout() {
  state.token = null;
  state.user = null;
  state.savedDashboards = [];
  state.preferences = null;
  localStorage.removeItem(AUTH_TOKEN_KEY);
  document.body.classList.remove("focus-mode", "compact-view");
  renderSignedOutWorkspace();
}

async function loadUserWorkspace() {
  if (!state.token) {
    return;
  }
  const [dashboards, preferences] = await Promise.all([
    apiRequest("/api/dashboards"),
    apiRequest("/api/preferences"),
  ]);
  state.savedDashboards = dashboards;
  state.preferences = preferences;
  renderSavedDashboards();
  renderPreferences(preferences);
}

async function saveCurrentDashboard() {
  if (!requireAuth()) {
    return;
  }
  const name = elements.dashboardName.value.trim();
  if (!name) {
    setAccountStatus("Name this dashboard before saving.", true);
    return;
  }
  try {
    await apiRequest("/api/dashboards", {
      method: "POST",
      body: JSON.stringify({
        name,
        description: "Saved from InsightFlow dashboard",
        filters: currentFilters(),
      }),
    });
    elements.dashboardName.value = "";
    setAccountStatus("Dashboard saved.");
    await loadUserWorkspace();
  } catch (error) {
    setAccountStatus(error.message, true);
  }
}

function loadSavedDashboardView() {
  const dashboard = selectedDashboard();
  if (!dashboard) {
    setAccountStatus("Select a saved dashboard to load.", true);
    return;
  }
  applyFilters(dashboard.filters || {});
  elements.dashboardName.value = dashboard.name;
  loadDashboard();
}

async function deleteSavedDashboardView() {
  const dashboard = selectedDashboard();
  if (!dashboard || !requireAuth()) {
    return;
  }
  try {
    await apiRequest(`/api/dashboards/${dashboard.id}`, { method: "DELETE" });
    setAccountStatus("Saved dashboard deleted.");
    await loadUserWorkspace();
  } catch (error) {
    setAccountStatus(error.message, true);
  }
}

async function saveDashboardPreferences() {
  if (!requireAuth()) {
    return;
  }
  try {
    const preferences = await apiRequest("/api/preferences", {
      method: "PUT",
      body: JSON.stringify({
        theme: elements.preferenceTheme.value,
        compactView: elements.compactView.checked,
        defaultDashboardId: elements.savedDashboards.value || null,
        visibleSections: ["kpis", "ai", "trend", "categories", "states", "reviews", "payments"],
      }),
    });
    state.preferences = preferences;
    renderPreferences(preferences);
    setAccountStatus("Preferences saved.");
  } catch (error) {
    setAccountStatus(error.message, true);
  }
}

async function loadDashboard() {
  showLoading();
  try {
    const summary = await loadSummary();
    state.latestSummary = summary;
    elements.exportCsv.disabled = false;
    hydrateFilterOptions(summary.filterOptions);
    resetAiOutputs();

    if (summary.empty) {
      renderEmpty(summary);
      return;
    }

    hideMessage();
    renderDashboard(summary);
  } catch (error) {
    state.latestSummary = null;
    renderError(error);
  }
}

function showLoading() {
  elements.generatedAt.textContent = "Loading from API";
  elements.sourceMode.textContent = "Connecting";
  elements.sourceMode.className = "mode-badge loading";
  elements.exportCsv.disabled = true;
  elements.message.hidden = true;
  elements.kpiGrid.innerHTML = Array.from({ length: 6 }, () => `<article class="kpi skeleton"></article>`).join("");
  elements.monthlyChart.innerHTML = loadingState("Loading revenue trend");
  elements.categoryList.innerHTML = loadingState("Loading categories");
  elements.stateList.innerHTML = loadingState("Loading states");
  elements.reviewChart.innerHTML = loadingState("Loading reviews");
  elements.paymentList.innerHTML = loadingState("Loading payments");
}

async function generateExecutiveReport() {
  elements.generateReport.disabled = true;
  elements.reportMode.textContent = "Generating";
  elements.reportMode.className = "ai-mode";
  elements.reportOutput.innerHTML = loadingState("Generating executive report");

  try {
    const report = await postAi("/api/ai/report", { filters: currentFilters() });
    renderExecutiveReport(report);
  } catch (error) {
    elements.reportMode.textContent = "Unavailable";
    elements.reportMode.className = "ai-mode";
    elements.reportOutput.innerHTML = emptyState("Could not generate report", error.message);
  } finally {
    elements.generateReport.disabled = false;
  }
}

async function askBusinessAnalyst(event) {
  event?.preventDefault();
  const question = elements.analystQuestion.value.trim();
  if (!question) {
    elements.queryOutput.innerHTML = emptyState("Question required", "Enter an analytics question.");
    return;
  }

  elements.askAnalyst.disabled = true;
  setPromptChipsDisabled(true);
  elements.queryMode.textContent = "Thinking";
  elements.queryMode.className = "ai-mode";
  elements.queryOutput.innerHTML = loadingState("Analyzing current view");

  try {
    const response = await postAi("/api/ai/query", {
      question,
      filters: currentFilters(),
    });
    renderAnalystAnswer(response);
  } catch (error) {
    elements.queryMode.textContent = "Unavailable";
    elements.queryMode.className = "ai-mode";
    elements.queryOutput.innerHTML = emptyState("Could not answer question", error.message);
  } finally {
    elements.askAnalyst.disabled = false;
    setPromptChipsDisabled(false);
  }
}

function askSuggestedQuestion(question) {
  if (!question) {
    return;
  }
  elements.analystQuestion.value = question;
  askBusinessAnalyst();
}

function setPromptChipsDisabled(disabled) {
  elements.promptChips.forEach((button) => {
    button.disabled = disabled;
  });
}

function renderError(error) {
  elements.generatedAt.textContent = "API unavailable";
  elements.sourceMode.textContent = "Offline";
  elements.sourceMode.className = "mode-badge error";
  elements.exportCsv.disabled = true;
  elements.kpiGrid.innerHTML = "";
  clearCharts("Unable to load analytics", "Start the Spring Boot API and try again.");
  elements.message.hidden = false;
  elements.message.className = "message-panel error";
  elements.message.innerHTML = `
    <div>
      <strong>Could not load analytics</strong>
      <span>${escapeHtml(error.message)}</span>
    </div>
    <button type="button" id="retryLoad">Retry</button>
  `;
  document.querySelector("#retryLoad").addEventListener("click", loadDashboard);
}

function renderEmpty(summary) {
  renderKpis(summary.kpis);
  renderSourceMode(summary);
  clearCharts("No matching data", "Adjust the filters to broaden the analysis window.");
  elements.trendNote.textContent = "";
  elements.generatedAt.textContent = generatedLabel(summary.generatedAt);
  elements.message.hidden = false;
  elements.message.className = "message-panel";
  elements.message.innerHTML = `
    <div>
      <strong>No orders match these filters</strong>
      <span>Try a wider date range or remove one of the selected filters.</span>
    </div>
  `;
}

function hideMessage() {
  elements.message.hidden = true;
  elements.message.innerHTML = "";
}

function clearCharts(title = "No data", note = "There is no chart data for the selected filters.") {
  const empty = emptyState(title, note);
  elements.trendNote.textContent = "";
  elements.monthlyChart.innerHTML = empty;
  elements.categoryList.innerHTML = empty;
  elements.stateList.innerHTML = empty;
  elements.reviewChart.innerHTML = empty;
  elements.paymentList.innerHTML = empty;
}

function renderDashboard(summary) {
  renderKpis(summary.kpis);
  renderSourceMode(summary);
  renderMonthlyChart(summary.revenueByMonth || []);
  renderHorizontalBars(elements.categoryList, summary.topCategories || [], {
    labelKey: "category",
    valueKey: "revenue",
    detail: (row) =>
      `${currency.format(asNumber(row.revenue))}<br>${number.format(asNumber(row.orders))} orders<br>${number.format(asNumber(row.items))} items`,
    emptyTitle: "No category revenue",
    maxRows: 10,
  });
  renderHorizontalBars(elements.stateList, summary.revenueByState || [], {
    labelKey: "state",
    valueKey: "revenue",
    detail: (row) => `${currency.format(asNumber(row.revenue))}<br>${number.format(asNumber(row.orders))} orders`,
    emptyTitle: "No state revenue",
    maxRows: 12,
    compactLabels: true,
  });
  renderReviews(summary.reviewDistribution || []);
  renderPayments(summary.paymentMethodBreakdown || []);
  elements.generatedAt.textContent = generatedLabel(summary.generatedAt);
}

function resetAiOutputs() {
  elements.reportMode.textContent = "Local analyst ready";
  elements.reportMode.className = "ai-mode local";
  elements.reportOutput.innerHTML = emptyState(
    "Executive brief ready",
    "Current dashboard view awaiting analysis.",
  );
  elements.queryMode.textContent = "Local analyst ready";
  elements.queryMode.className = "ai-mode local";
  elements.queryOutput.innerHTML = "";
}

function renderExecutiveReport(report) {
  elements.reportMode.textContent = analystMode(report.mode);
  elements.reportMode.className = `ai-mode ${report.mode === "openai" ? "openai" : "local"}`;
  elements.reportOutput.innerHTML = `
    <section>
      <h3>Executive Summary</h3>
      <p>${escapeHtml(report.executiveSummary)}</p>
    </section>
    ${renderAiList("Key Findings", report.keyFindings)}
    ${renderAiList("Risks", report.risks)}
    ${renderAiList("Recommendations", report.recommendations)}
  `;
}

function renderAnalystAnswer(response) {
  elements.queryMode.textContent = analystMode(response.mode);
  elements.queryMode.className = `ai-mode ${response.mode === "openai" ? "openai" : "local"}`;
  const question = elements.analystQuestion.value.trim();
  elements.queryOutput.innerHTML = `
    <section class="analyst-answer-card">
      <div class="answer-header">
        <span class="metric-icon" aria-hidden="true">${iconSvg("sparkles")}</span>
        <div>
          <h3>${escapeHtml(question || "Answer")}</h3>
          <span>${escapeHtml(filterContextLabel())}</span>
        </div>
      </div>
      <p>${escapeHtml(response.answer)}</p>
    </section>
    ${renderAiList("Supporting Metrics", response.supportingMetrics, "metric-list")}
    ${renderFollowUpQuestions(response.followUpQuestions)}
  `;
  bindFollowUpQuestions();
}

function renderAiList(title, rows = [], className = "") {
  if (!rows.length) {
    return "";
  }
  return `
    <section class="${escapeAttr(className)}">
      <h3>${escapeHtml(title)}</h3>
      <ul>
        ${rows.map((row) => `<li>${escapeHtml(row)}</li>`).join("")}
      </ul>
    </section>
  `;
}

function renderFollowUpQuestions(rows = []) {
  if (!rows.length) {
    return "";
  }
  return `
    <section>
      <h3>Follow-Up Questions</h3>
      <div class="follow-up-grid">
        ${rows
          .map(
            (row) => `
              <button type="button" class="follow-up-question" data-question="${escapeAttr(row)}">
                ${iconSvg("send", "button-icon")}
                <span>${escapeHtml(row)}</span>
              </button>
            `,
          )
          .join("")}
      </div>
    </section>
  `;
}

function bindFollowUpQuestions() {
  elements.queryOutput.querySelectorAll(".follow-up-question").forEach((button) => {
    button.addEventListener("click", () => askSuggestedQuestion(button.dataset.question || ""));
  });
}

function filterContextLabel() {
  const filters = currentFilters();
  const active = [
    filters.startDate && filters.endDate ? `${filters.startDate} to ${filters.endDate}` : null,
    filters.state ? `State ${filters.state}` : null,
    filters.category ? filters.category : null,
    filters.paymentType ? filters.paymentType : null,
  ].filter(Boolean);
  return active.length ? active.join(" · ") : "All orders";
}

function renderSourceMode(summary) {
  const source = String(summary.source || "").toLowerCase();
  const isPostgres = source.includes("postgres");
  elements.sourceMode.textContent = isPostgres ? "PostgreSQL mode" : "JSON fallback";
  elements.sourceMode.className = `mode-badge ${isPostgres ? "postgres" : "json"}`;
  elements.sourceMode.title = summary.source || elements.sourceMode.textContent;
}

function renderSignedInWorkspace() {
  elements.authForm.hidden = true;
  elements.logoutUser.hidden = false;
  setWorkspaceDisabled(false);
  setAccountStatus(`Signed in as ${state.user.displayName} (${state.user.email})`);
}

function renderSignedOutWorkspace() {
  elements.authForm.hidden = false;
  elements.logoutUser.hidden = true;
  setWorkspaceDisabled(true);
  elements.savedDashboards.innerHTML = `<option value="">Sign in to save dashboards</option>`;
  elements.preferenceTheme.value = "light";
  elements.compactView.checked = false;
  setAccountStatus("Sign in to save dashboards and preferences.");
}

function setWorkspaceDisabled(disabled) {
  elements.dashboardName.disabled = disabled;
  elements.saveDashboard.disabled = disabled;
  elements.savedDashboards.disabled = disabled;
  elements.loadDashboardView.disabled = disabled;
  elements.deleteDashboardView.disabled = disabled;
  elements.preferenceTheme.disabled = disabled;
  elements.compactView.disabled = disabled;
  elements.savePreferences.disabled = disabled;
}

function setAccountStatus(message, isError = false) {
  elements.accountStatus.textContent = message;
  elements.accountStatus.classList.toggle("error", isError);
}

function renderSavedDashboards() {
  if (!state.savedDashboards.length) {
    elements.savedDashboards.innerHTML = `<option value="">No saved dashboards</option>`;
    return;
  }
  const defaultId = state.preferences?.defaultDashboardId || "";
  elements.savedDashboards.innerHTML = state.savedDashboards
    .map(
      (dashboard) =>
        `<option value="${escapeAttr(dashboard.id)}">${escapeHtml(dashboard.name)}</option>`,
    )
    .join("");
  elements.savedDashboards.value = defaultId || state.savedDashboards[0].id;
}

function renderPreferences(preferences) {
  elements.preferenceTheme.value = preferences.theme || "light";
  elements.compactView.checked = Boolean(preferences.compactView);
  document.body.classList.toggle("focus-mode", preferences.theme === "focus");
  document.body.classList.toggle("compact-view", Boolean(preferences.compactView));
  renderSavedDashboards();
}

function selectedDashboard() {
  return state.savedDashboards.find((dashboard) => dashboard.id === elements.savedDashboards.value);
}

function requireAuth() {
  if (state.token) {
    return true;
  }
  setAccountStatus("Sign in before using workspace features.", true);
  return false;
}

function applyFilters(filters) {
  setFilterValue("#startDate", filters.startDate);
  setFilterValue("#endDate", filters.endDate);
  setFilterValue("#state", filters.state);
  setFilterValue("#category", filters.category);
  setFilterValue("#paymentType", filters.paymentType);
}

function setFilterValue(selector, value) {
  document.querySelector(selector).value = value || "";
}

function hydrateFilterOptions(options) {
  if (!options || state.filtersLoaded) {
    return;
  }

  document.querySelector("#startDate").min = options.minDate || "";
  document.querySelector("#startDate").max = options.maxDate || "";
  document.querySelector("#endDate").min = options.minDate || "";
  document.querySelector("#endDate").max = options.maxDate || "";

  fillSelect("#state", options.states || [], "All states");
  fillSelect("#category", options.categories || [], "All categories");
  fillSelect("#paymentType", options.paymentTypes || [], "All payments");
  state.filtersLoaded = true;
}

function fillSelect(selector, values, fallbackLabel) {
  const select = document.querySelector(selector);
  const currentValue = select.value;
  select.innerHTML = `<option value="">${fallbackLabel}</option>${values
    .map((value) => `<option value="${escapeAttr(value)}">${escapeHtml(value)}</option>`)
    .join("")}`;
  select.value = currentValue;
}

function renderKpis(kpis) {
  elements.kpiGrid.innerHTML = kpiConfig
    .map(([key, label, formatter, note, icon]) => {
      return `
        <article class="kpi">
          <div class="kpi-label">
            <span class="metric-icon" aria-hidden="true">${iconSvg(icon)}</span>
            <span>${label}</span>
          </div>
          <strong>${formatter(kpis?.[key])}</strong>
          <em>${note}</em>
        </article>
      `;
    })
    .join("");
}

function renderMonthlyChart(monthlySales) {
  const rows = monthlySales.map((row) => ({ ...row, revenue: asNumber(row.revenue), orders: asNumber(row.orders) }));
  if (!rows.length) {
    elements.monthlyChart.innerHTML = emptyState("No monthly revenue", "No delivered orders match this time period.");
    elements.trendNote.textContent = "";
    return;
  }

  const width = 960;
  const height = 300;
  const padding = { top: 18, right: 28, bottom: 42, left: 70 };
  const values = rows.map((item) => item.revenue);
  const max = Math.max(...values);
  const min = Math.min(...values);
  const plotWidth = width - padding.left - padding.right;
  const plotHeight = height - padding.top - padding.bottom;
  const denominator = Math.max(1, max - min);
  const points = rows.map((item, index) => {
    const x = padding.left + (plotWidth * index) / Math.max(1, rows.length - 1);
    const y =
      max === min
        ? padding.top + plotHeight / 2
        : padding.top + plotHeight - ((item.revenue - min) / denominator) * plotHeight;
    return { ...item, x, y };
  });

  const baseline = height - padding.bottom;
  const area = [
    `${points[0].x},${baseline}`,
    ...points.map((point) => `${point.x},${point.y}`),
    `${points[points.length - 1].x},${baseline}`,
  ].join(" ");
  const trend = points[points.length - 1].revenue - points[0].revenue;
  elements.trendNote.textContent =
    trend >= 0
      ? `${shortCurrency.format(trend)} higher than first month`
      : `${shortCurrency.format(Math.abs(trend))} lower than first month`;

  const labelStep = Math.max(1, Math.ceil(points.length / 8));
  const gridLines = [0, 0.25, 0.5, 0.75, 1]
    .map((ratio) => {
      const y = padding.top + plotHeight * ratio;
      const value = max - (max - min) * ratio;
      return `
        <line class="chart-grid" x1="${padding.left}" y1="${y}" x2="${width - padding.right}" y2="${y}" />
        <text class="axis-label" x="12" y="${y + 4}">${shortCurrency.format(value)}</text>
      `;
    })
    .join("");

  elements.monthlyChart.innerHTML = `
    <svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Monthly revenue line chart">
      ${gridLines}
      <line class="chart-axis" x1="${padding.left}" y1="${baseline}" x2="${width - padding.right}" y2="${baseline}" />
      <line class="chart-axis" x1="${padding.left}" y1="${padding.top}" x2="${padding.left}" y2="${baseline}" />
      <polygon class="chart-area" points="${area}"></polygon>
      <polyline class="chart-line" points="${points.map((point) => `${point.x},${point.y}`).join(" ")}"></polyline>
      ${points
        .map(
          (point, index) => `
            <circle class="chart-dot" cx="${point.x}" cy="${point.y}" r="5.5" fill="${chartColors[0]}"
              data-tooltip="${escapeAttr(`${point.month}\n${currency.format(point.revenue)}\n${number.format(point.orders)} orders`)}"></circle>
            ${
              index % labelStep === 0 || index === points.length - 1
                ? `<text class="axis-label" x="${point.x}" y="${height - 14}" text-anchor="middle">${escapeHtml(point.month.slice(2))}</text>`
                : ""
            }
          `,
        )
        .join("")}
    </svg>
  `;
  bindChartTooltip(elements.monthlyChart);
}

function renderHorizontalBars(container, rows, config) {
  const visibleRows = rows
    .slice(0, config.maxRows || rows.length)
    .map((row) => ({ ...row, [config.valueKey]: asNumber(row[config.valueKey]) }));

  if (!visibleRows.length) {
    container.innerHTML = emptyState(config.emptyTitle || "No data", "No records match the selected filters.");
    return;
  }

  const width = 820;
  const rowHeight = config.compactLabels ? 24 : 30;
  const padding = {
    top: 12,
    right: 106,
    bottom: 24,
    left: config.compactLabels ? 56 : 190,
  };
  const height = padding.top + padding.bottom + visibleRows.length * rowHeight;
  const max = Math.max(...visibleRows.map((row) => row[config.valueKey]));
  const barWidth = width - padding.left - padding.right;

  container.innerHTML = `
    <svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Revenue bar chart">
      ${visibleRows
        .map((row, index) => {
          const y = padding.top + index * rowHeight;
          const barValue = max === 0 ? 0 : (row[config.valueKey] / max) * barWidth;
          const color = chartColors[index % chartColors.length];
          const label = String(row[config.labelKey] || "Unknown");
          return `
            <text class="axis-label" x="${padding.left - 12}" y="${y + 15}" text-anchor="end">${escapeHtml(truncate(label, config.compactLabels ? 7 : 24))}</text>
            <rect x="${padding.left}" y="${y + 2}" width="${barWidth}" height="14" rx="7" fill="#edf2ef"></rect>
            <rect class="chart-bar" x="${padding.left}" y="${y + 2}" width="${Math.max(2, barValue)}" height="14" rx="7" fill="${color}"
              data-tooltip="${escapeAttr(`${label}\n${stripTags(config.detail(row))}`)}"></rect>
            <text class="axis-label" x="${width - 12}" y="${y + 15}" text-anchor="end">${shortCurrency.format(row[config.valueKey])}</text>
          `;
        })
        .join("")}
    </svg>
  `;
  bindChartTooltip(container);
}

function renderReviews(reviewDistribution) {
  const rows = reviewDistribution.map((row) => ({ ...row, count: asNumber(row.count) }));
  if (!rows.length) {
    elements.reviewChart.innerHTML = emptyState("No reviews", "No review scores match these filters.");
    return;
  }

  const width = 620;
  const height = 260;
  const padding = { top: 18, right: 18, bottom: 40, left: 46 };
  const plotWidth = width - padding.left - padding.right;
  const plotHeight = height - padding.top - padding.bottom;
  const max = Math.max(...rows.map((row) => row.count));
  const barGap = 14;
  const barWidth = (plotWidth - barGap * (rows.length - 1)) / rows.length;
  const total = rows.reduce((sum, row) => sum + row.count, 0);

  elements.reviewChart.innerHTML = `
    <svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Review score distribution chart">
      <line class="chart-axis" x1="${padding.left}" y1="${height - padding.bottom}" x2="${width - padding.right}" y2="${height - padding.bottom}" />
      ${rows
        .map((row, index) => {
          const barHeight = max === 0 ? 0 : (row.count / max) * plotHeight;
          const x = padding.left + index * (barWidth + barGap);
          const y = height - padding.bottom - barHeight;
          const share = total === 0 ? 0 : (row.count / total) * 100;
          return `
            <rect class="chart-bar" x="${x}" y="${y}" width="${barWidth}" height="${Math.max(3, barHeight)}" rx="7" fill="${chartColors[index % chartColors.length]}"
              data-tooltip="${escapeAttr(`${row.score} star\n${number.format(row.count)} reviews\n${percent.format(share)}% of reviews`)}"></rect>
            <text class="axis-label" x="${x + barWidth / 2}" y="${height - 15}" text-anchor="middle">${row.score}</text>
            <text class="axis-label" x="${x + barWidth / 2}" y="${Math.max(14, y - 7)}" text-anchor="middle">${number.format(row.count)}</text>
          `;
        })
        .join("")}
    </svg>
  `;
  bindChartTooltip(elements.reviewChart);
}

function renderPayments(paymentMix) {
  const rows = paymentMix.map((row) => ({ ...row, value: asNumber(row.value), share: asNumber(row.share) }));
  if (!rows.length) {
    elements.paymentList.innerHTML = emptyState("No payment data", "No payment records match these filters.");
    return;
  }

  const topPayment = rows[0];
  let offset = 0;
  const segments = rows
    .map((row, index) => {
      const color = chartColors[index % chartColors.length];
      const segment = `
        <circle cx="120" cy="120" r="82" fill="none" stroke="${color}" stroke-width="34" pathLength="100"
          stroke-dasharray="${Math.max(0, row.share)} ${Math.max(0, 100 - row.share)}"
          stroke-dashoffset="${-offset}"
          transform="rotate(-90 120 120)"
          data-tooltip="${escapeAttr(`${row.paymentType}\n${currency.format(row.value)}\n${percent.format(row.share)}%`)}"></circle>
      `;
      offset += row.share;
      return segment;
    })
    .join("");

  elements.paymentList.innerHTML = `
    <svg viewBox="0 0 520 260" role="img" aria-label="Payment method breakdown donut chart">
      <circle cx="120" cy="120" r="82" fill="none" stroke="#edf2ef" stroke-width="34"></circle>
      ${segments}
      <circle cx="120" cy="120" r="50" fill="#fff"></circle>
      <text x="120" y="112" text-anchor="middle" font-size="15" font-weight="800" fill="#17211b">${escapeHtml(truncate(topPayment.paymentType, 13))}</text>
      <text class="axis-label" x="120" y="134" text-anchor="middle">${percent.format(topPayment.share)}%</text>
      <foreignObject x="240" y="24" width="260" height="212">
        <div class="legend-list" xmlns="http://www.w3.org/1999/xhtml">
          ${rows
            .map(
              (row, index) => `
                <div class="legend-row">
                  <span class="legend-swatch" style="background:${chartColors[index % chartColors.length]}"></span>
                  <span class="legend-label" title="${escapeAttr(row.paymentType)}">${escapeHtml(row.paymentType)}</span>
                  <span class="legend-value">${percent.format(row.share)}%</span>
                </div>
              `,
            )
            .join("")}
        </div>
      </foreignObject>
    </svg>
  `;
  bindChartTooltip(elements.paymentList);
}

function exportCurrentCsv() {
  if (!state.latestSummary) {
    return;
  }

  const summary = state.latestSummary;
  const rows = [];
  addSection(rows, "Filters", [
    ["Field", "Value"],
    ["Start date", summary.filters?.startDate || ""],
    ["End date", summary.filters?.endDate || ""],
    ["Customer state", summary.filters?.state || "All"],
    ["Product category", summary.filters?.category || "All"],
    ["Payment type", summary.filters?.paymentType || "All"],
    ["Data source", sourceModeLabel(summary)],
  ]);
  addSection(rows, "KPI Summary", [
    ["Metric", "Value"],
    ...kpiConfig.map(([key, label, formatter]) => [label, formatter(summary.kpis?.[key])]),
  ]);
  addSection(rows, "Revenue By Month", [
    ["Month", "Revenue", "Orders"],
    ...(summary.revenueByMonth || []).map((row) => [row.month, asNumber(row.revenue), asNumber(row.orders)]),
  ]);
  addSection(rows, "Top Product Categories", [
    ["Category", "Revenue", "Orders", "Items", "Average Review Score"],
    ...(summary.topCategories || []).map((row) => [
      row.category,
      asNumber(row.revenue),
      asNumber(row.orders),
      asNumber(row.items),
      row.averageReviewScore ?? "",
    ]),
  ]);
  addSection(rows, "Revenue By State", [
    ["State", "Revenue", "Orders"],
    ...(summary.revenueByState || []).map((row) => [row.state, asNumber(row.revenue), asNumber(row.orders)]),
  ]);
  addSection(rows, "Payment Method Breakdown", [
    ["Payment Type", "Value", "Share"],
    ...(summary.paymentMethodBreakdown || []).map((row) => [row.paymentType, asNumber(row.value), `${asNumber(row.share)}%`]),
  ]);
  addSection(rows, "Review Score Distribution", [
    ["Score", "Count"],
    ...(summary.reviewDistribution || []).map((row) => [row.score, asNumber(row.count)]),
  ]);

  const csv = rows.map((row) => row.map(csvCell).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `insightflow-analytics-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function currentFilters() {
  if (state.latestSummary?.filters) {
    return {
      startDate: state.latestSummary.filters.startDate || null,
      endDate: state.latestSummary.filters.endDate || null,
      state: state.latestSummary.filters.state || null,
      category: state.latestSummary.filters.category || null,
      paymentType: state.latestSummary.filters.paymentType || null,
    };
  }

  const formData = new FormData(elements.form);
  return {
    startDate: cleanFilterValue(formData.get("startDate")),
    endDate: cleanFilterValue(formData.get("endDate")),
    state: cleanFilterValue(formData.get("state")),
    category: cleanFilterValue(formData.get("category")),
    paymentType: cleanFilterValue(formData.get("paymentType")),
  };
}

function cleanFilterValue(value) {
  const text = String(value || "").trim();
  return text ? text : null;
}

function addSection(rows, title, sectionRows) {
  if (rows.length) {
    rows.push([]);
  }
  rows.push([title]);
  rows.push(...sectionRows);
}

function bindChartTooltip(container) {
  const tooltip = ensureTooltip(container);
  container.querySelectorAll("[data-tooltip]").forEach((node) => {
    node.addEventListener("pointermove", (event) => {
      const bounds = container.getBoundingClientRect();
      tooltip.innerHTML = escapeHtml(node.dataset.tooltip || "").replaceAll("\n", "<br>");
      tooltip.style.left = `${event.clientX - bounds.left}px`;
      tooltip.style.top = `${event.clientY - bounds.top}px`;
      tooltip.classList.add("visible");
    });
    node.addEventListener("pointerleave", () => {
      tooltip.classList.remove("visible");
    });
  });
}

function ensureTooltip(container) {
  let tooltip = container.querySelector(".chart-tooltip");
  if (!tooltip) {
    tooltip = document.createElement("div");
    tooltip.className = "chart-tooltip";
    container.append(tooltip);
  }
  return tooltip;
}

function loadingState(label) {
  return `<div class="chart-state loading-state"><span class="loading-mark" aria-hidden="true"></span>${escapeHtml(label)}</div>`;
}

function emptyState(title, note) {
  return `<div class="chart-state"><strong>${escapeHtml(title)}</strong><span>${escapeHtml(note)}</span></div>`;
}

function sourceModeLabel(summary) {
  return String(summary.source || "").toLowerCase().includes("postgres") ? "PostgreSQL mode" : "JSON fallback mode";
}

function analystMode(mode) {
  return mode === "openai" ? "OpenAI analyst" : "Local analyst";
}

function generatedLabel(value) {
  if (!value) {
    return "Generated timestamp unavailable";
  }
  return `Generated ${new Date(value).toLocaleString()}`;
}

function asNumber(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function truncate(value, limit) {
  const text = String(value ?? "");
  return text.length > limit ? `${text.slice(0, Math.max(0, limit - 1))}...` : text;
}

function stripTags(value) {
  return String(value).replace(/<[^>]*>/g, "\n");
}

function csvCell(value) {
  return `"${String(value ?? "").replaceAll('"', '""')}"`;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function escapeAttr(value) {
  return escapeHtml(value).replaceAll("\n", "&#10;");
}

function enhanceButtonIcons() {
  document.querySelectorAll("button[data-icon]").forEach((button) => {
    const icon = button.dataset.icon;
    const label = button.textContent.trim();
    button.innerHTML = `${iconSvg(icon, "button-icon")}<span>${escapeHtml(label)}</span>`;
  });
}

function iconSvg(name, className = "icon") {
  const paths = icons[name] || icons.filter;
  return `
    <svg class="${className}" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      ${paths}
    </svg>
  `;
}

window.insightFlowDashboard = {
  loadDashboard,
  buildQuery,
  exportCurrentCsv,
  generateExecutiveReport,
  restoreSession,
};

restoreSession();
loadDashboard();
