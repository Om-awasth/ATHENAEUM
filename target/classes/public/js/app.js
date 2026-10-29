/**
 * ATHENAEUM LIBRARY MANAGEMENT SYSTEM — FRONTEND JAVASCRIPT
 * Pure Vanilla JavaScript (ES6+), Event-driven architecture
 * Demonstrates DOM Event Handling, Debounced Live Search, SVG Charts, and Modals
 */

// Application State
const state = {
    currentUser: null,
    currentView: 'dashboard',
    catalogViewMode: localStorage.getItem('athenaeum_view_mode') || 'grid',
    catalogItems: [],
    categories: [],
    members: [],
    activeLoans: [],
    overdueLoans: [],
    reservations: [],
    stats: {},
    searchDebounceTimer: null
};

// =============================================================================
// DOM CONTENT LOADED & INITIALIZATION
// =============================================================================
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

async function initApp() {
    updateDateDisplay();
    setupGlobalEventListeners();
    await checkAuthSession();
}

function updateDateDisplay() {
    const el = document.getElementById('currentDateDisplay');
    if (el) {
        const options = { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' };
        el.textContent = new Date().toLocaleDateString('en-IN', options);
    }
}

// =============================================================================
// EVENT LISTENERS & KEYBOARD ACCESSIBILITY
// =============================================================================
function setupGlobalEventListeners() {
    // Keyboard shortcut '/' to focus global search
    document.addEventListener('keydown', (e) => {
        if (e.key === '/' && document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
            e.preventDefault();
            const searchInput = document.getElementById('globalSearchInput');
            if (searchInput) searchInput.focus();
        } else if (e.key === 'Escape') {
            closeAllModals();
        }
    });

    // Global Search input with debounce
    const globalSearch = document.getElementById('globalSearchInput');
    if (globalSearch) {
        globalSearch.addEventListener('input', (e) => {
            clearTimeout(state.searchDebounceTimer);
            state.searchDebounceTimer = setTimeout(() => {
                const q = e.target.value.trim();
                if (q.length > 0) {
                    switchView('books');
                    const catalogSearch = document.getElementById('catalogSearchInput');
                    if (catalogSearch) {
                        catalogSearch.value = q;
                        fetchCatalog();
                    }
                }
            }, 300);
        });
    }

    // Catalog Search input with debounce
    const catalogSearch = document.getElementById('catalogSearchInput');
    if (catalogSearch) {
        catalogSearch.addEventListener('input', () => {
            clearTimeout(state.searchDebounceTimer);
            state.searchDebounceTimer = setTimeout(() => {
                fetchCatalog();
            }, 250);
        });
    }
}

// =============================================================================
// AUTHENTICATION & SESSION MANAGEMENT
// =============================================================================
async function checkAuthSession() {
    try {
        const res = await fetch('/api/auth/me');
        const json = await res.json();
        if (json.success && json.data) {
            handleAuthSuccess(json.data);
        } else {
            showAuthOverlay();
        }
    } catch (err) {
        showAuthOverlay();
    }
}

function showAuthOverlay() {
    document.getElementById('authOverlay').classList.remove('hidden');
}

function hideAuthOverlay() {
    document.getElementById('authOverlay').classList.add('hidden');
}

function fillCredentials(email, password) {
    document.getElementById('loginEmail').value = email;
    document.getElementById('loginPassword').value = password;
}

async function handleLoginSubmit() {
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const json = await res.json();

        if (json.success) {
            hideAuthOverlay();
            handleAuthSuccess(json.data);
            showToast('Welcome, ' + json.data.name + '!', 'success');
        } else {
            showToast(json.message || 'Login failed', 'error');
        }
    } catch (err) {
        showToast('Network error during login', 'error');
    }
}

function handleAuthSuccess(user) {
    state.currentUser = user;
    document.getElementById('userNameLabel').textContent = user.name;
    document.getElementById('userRoleLabel').textContent = user.role === 'LIBRARIAN' ? 'Librarian (Staff)' : 'Member (' + (user.membershipId || '') + ')';
    document.getElementById('userAvatar').textContent = user.name.charAt(0).toUpperCase();

    // Toggle librarian-only action buttons
    const isLibrarian = user.role === 'LIBRARIAN';
    document.querySelectorAll('.librarian-only').forEach(el => {
        if (isLibrarian) {
            el.classList.remove('hidden');
        } else {
            el.classList.add('hidden');
        }
    });

    // Refresh core datasets
    refreshAllData();
}

async function handleLogout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
        state.currentUser = null;
        showAuthOverlay();
        showToast('Signed out of repository', 'info');
    } catch (err) {
        showAuthOverlay();
    }
}

// =============================================================================
// VIEW NAVIGATION & SWITCHING
// =============================================================================
function switchView(viewName) {
    state.currentView = viewName;

    // Update active nav button
    document.querySelectorAll('.sidebar-nav .nav-item').forEach(btn => {
        if (btn.getAttribute('data-view') === viewName) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    // Update view panels
    document.querySelectorAll('.view-panel').forEach(panel => {
        panel.classList.remove('active');
    });

    const targetId = 'view' + viewName.charAt(0).toUpperCase() + viewName.slice(1);
    const targetPanel = document.getElementById(targetId);
    if (targetPanel) {
        targetPanel.classList.add('active');
    }

    // Close mobile sidebar if open
    const sidebar = document.getElementById('sidebar');
    if (sidebar) sidebar.classList.remove('mobile-open');

    // Trigger view-specific data refresh
    switch (viewName) {
        case 'dashboard': fetchDashboardStats(); break;
        case 'books': fetchCatalog(); break;
        case 'circulation': fetchCirculationDesk(); break;
        case 'overdue': fetchOverdueLoans(); break;
        case 'members': fetchMembers(); break;
        case 'reservations': fetchReservations(); break;
    }
}

function toggleMobileSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) sidebar.classList.toggle('mobile-open');
}

// =============================================================================
// DATA FETCHING & SYNCHRONIZATION
// =============================================================================
async function refreshAllData() {
    await Promise.all([
        fetchDashboardStats(),
        fetchCategories(),
        fetchMembers()
    ]);
}

// 1. Dashboard
async function fetchDashboardStats() {
    try {
        const res = await fetch('/api/stats');
        const json = await res.json();
        if (json.success && json.data) {
            state.stats = json.data;
            renderDashboardStats(json.data);
            renderCategoryChart(json.data.categoryDistribution || {});
            renderVelocityChart(json.data);
        }

        // Fetch urgent overdue items for dashboard table
        const overdueRes = await fetch('/api/loans/overdue');
        const overdueJson = await overdueRes.json();
        if (overdueJson.success) {
            state.overdueLoans = overdueJson.data || [];
            renderDashboardOverdue(state.overdueLoans.slice(0, 5));

            // Update sidebar overdue badge
            const badge = document.getElementById('sidebarOverdueCount');
            if (badge) {
                if (state.overdueLoans.length > 0) {
                    badge.textContent = state.overdueLoans.length;
                    badge.classList.remove('hidden');
                } else {
                    badge.classList.add('hidden');
                }
            }
        }

        // Fetch recent transactions for dashboard stream
        const txRes = await fetch('/api/loans/history');
        const txJson = await txRes.json();
        if (txJson.success) {
            renderDashboardActivity(txJson.data ? txJson.data.slice(0, 5) : []);
        }
    } catch (err) {
        console.error('Failed fetching dashboard statistics', err);
    }
}

function renderDashboardStats(stats) {
    document.getElementById('statTotalBooks').textContent = stats.totalBooks || 0;
    document.getElementById('statTotalCopies').textContent = (stats.totalCopies || 0) + ' total copies';
    document.getElementById('statAvailableCopies').textContent = stats.availableCopies || 0;
    document.getElementById('statIssuedCount').textContent = stats.issuedCount || 0;
    document.getElementById('statOverdueCount').textContent = stats.overdueCount || 0;
    document.getElementById('statMemberCount').textContent = stats.memberCount || 0;
}

// Handcrafted Pure SVG Bar Chart for Category Distribution
function renderCategoryChart(distribution) {
    const container = document.getElementById('categoryChartContainer');
    if (!container) return;

    const entries = Object.entries(distribution);
    if (entries.length === 0) {
        container.innerHTML = '<p class="text-muted text-center">No categories recorded</p>';
        return;
    }

    const svgWidth = 520;
    const svgHeight = 220;
    const padding = { top: 20, right: 20, bottom: 40, left: 40 };
    const chartW = svgWidth - padding.left - padding.right;
    const chartH = svgHeight - padding.top - padding.bottom;

    const maxVal = Math.max(...entries.map(e => e[1]), 5);
    const barWidth = Math.min(38, (chartW / entries.length) - 14);

    let barsSvg = '';
    entries.forEach((entry, i) => {
        const [label, count] = entry;
        const x = padding.left + i * (chartW / entries.length) + ((chartW / entries.length) - barWidth) / 2;
        const h = (count / maxVal) * chartH;
        const y = padding.top + chartH - h;

        // Truncate long labels for display
        const displayLabel = label.length > 10 ? label.slice(0, 9) + '…' : label;

        barsSvg += `
            <g class="chart-bar-group">
                <rect x="${x}" y="${y}" width="${barWidth}" height="${h}" rx="3" fill="#1E3326" class="svg-bar">
                    <title>${label}: ${count} holdings</title>
                </rect>
                <text x="${x + barWidth / 2}" y="${y - 6}" text-anchor="middle" font-size="11" font-weight="600" fill="#1E3326">${count}</text>
                <text x="${x + barWidth / 2}" y="${svgHeight - 16}" text-anchor="middle" font-size="10.5" fill="#565E58">${displayLabel}</text>
            </g>
        `;
    });

    const axisLine = `<line x1="${padding.left}" y1="${padding.top + chartH}" x2="${svgWidth - padding.right}" y2="${padding.top + chartH}" stroke="#E2DDD3" stroke-width="1.5" />`;

    container.innerHTML = `
        <svg viewBox="0 0 ${svgWidth} ${svgHeight}" width="100%" height="100%">
            ${axisLine}
            ${barsSvg}
        </svg>
    `;
}

// Handcrafted Pure SVG Circulation Velocity Visual
function renderVelocityChart(stats) {
    const container = document.getElementById('velocityChartContainer');
    if (!container) return;

    const available = stats.availableCopies || 0;
    const issued = stats.issuedCount || 0;
    const overdue = stats.overdueCount || 0;
    const total = available + issued + overdue || 1;

    const availPct = Math.round((available / total) * 100);
    const issuedPct = Math.round((issued / total) * 100);
    const overduePct = Math.round((overdue / total) * 100);

    container.innerHTML = `
        <div style="width: 100%; display: flex; flex-direction: column; gap: 16px; justify-content: center;">
            <!-- Proportional Bar -->
            <div style="height: 24px; display: flex; border-radius: 4px; overflow: hidden; border: 1px solid var(--border);">
                <div style="width: ${availPct}%; background-color: var(--status-avail-text);" title="Available: ${availPct}%"></div>
                <div style="width: ${issuedPct}%; background-color: var(--status-issued-text);" title="Issued: ${issuedPct}%"></div>
                <div style="width: ${overduePct}%; background-color: var(--status-overdue-text);" title="Overdue: ${overduePct}%"></div>
            </div>

            <!-- Legend Grid -->
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; text-align: center;">
                <div style="background-color: var(--status-avail-bg); padding: 10px; border-radius: 4px;">
                    <span style="font-size: 0.72rem; color: var(--status-avail-text); font-weight: 600; text-transform: uppercase;">Available</span>
                    <p style="font-size: 1.25rem; font-weight: 700; color: var(--status-avail-text); margin-top: 2px;">${availPct}%</p>
                    <span style="font-size: 0.75rem; color: var(--text-muted);">${available} copies</span>
                </div>
                <div style="background-color: var(--status-issued-bg); padding: 10px; border-radius: 4px;">
                    <span style="font-size: 0.72rem; color: var(--status-issued-text); font-weight: 600; text-transform: uppercase;">Circulating</span>
                    <p style="font-size: 1.25rem; font-weight: 700; color: var(--status-issued-text); margin-top: 2px;">${issuedPct}%</p>
                    <span style="font-size: 0.75rem; color: var(--text-muted);">${issued} copies</span>
                </div>
                <div style="background-color: var(--status-overdue-bg); padding: 10px; border-radius: 4px;">
                    <span style="font-size: 0.72rem; color: var(--status-overdue-text); font-weight: 600; text-transform: uppercase;">Overdue</span>
                    <p style="font-size: 1.25rem; font-weight: 700; color: var(--status-overdue-text); margin-top: 2px;">${overduePct}%</p>
                    <span style="font-size: 0.75rem; color: var(--text-muted);">${overdue} loans</span>
                </div>
            </div>
        </div>
    `;
}

function renderDashboardOverdue(loans) {
    const tbody = document.getElementById('dashboardOverdueBody');
    if (!tbody) return;

    if (loans.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted" style="padding: 24px;">No overdue loans currently active! All items are within their loan periods.</td></tr>';
        return;
    }

    tbody.innerHTML = loans.map(loan => `
        <tr>
            <td style="font-weight: 600;">${escapeHtml(loan.itemTitle)}</td>
            <td>${escapeHtml(loan.memberName)}</td>
            <td class="text-danger" style="font-weight: 600;">${loan.dueDate}</td>
            <td style="font-weight: 600; color: var(--status-overdue-text);">₹ ${loan.fineAmount.toFixed(2)}</td>
            <td>
                <button class="btn btn-secondary btn-xs" onclick="processQuickReturn(${loan.id})">Return & Settle</button>
            </td>
        </tr>
    `).join('');
}

function renderDashboardActivity(transactions) {
    const container = document.getElementById('dashboardActivityStream');
    if (!container) return;

    if (transactions.length === 0) {
        container.innerHTML = '<p class="text-muted text-center" style="padding: 20px;">No recent circulation activity.</p>';
        return;
    }

    container.innerHTML = transactions.map(tx => {
        const isReturn = tx.status === 'RETURNED';
        const isLost = tx.status === 'LOST';
        const isOverdue = tx.status === 'OVERDUE';
        const iconSvg = isReturn 
            ? '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#21683E" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>'
            : isLost
            ? '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#858E88" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line></svg>'
            : '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#B88330" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>';

        return `
            <div class="activity-item">
                <div class="activity-icon">${iconSvg}</div>
                <div style="flex: 1;">
                    <span class="activity-title">${escapeHtml(tx.itemTitle)}</span>
                    <div class="activity-meta">
                        ${isReturn ? 'Returned by' : 'Checked out to'} <strong>${escapeHtml(tx.memberName)}</strong> · Due ${tx.dueDate}
                        ${tx.fineAmount > 0 ? `· <span class="text-danger">Fine: ₹${tx.fineAmount.toFixed(2)}</span>` : ''}
                    </div>
                </div>
                <span class="status-pill status-${tx.status.toLowerCase()}">${tx.status}</span>
            </div>
        `;
    }).join('');
}

// =============================================================================
// CATALOG (BOOKS & MATERIALS)
// =============================================================================
async function fetchCategories() {
    try {
        const res = await fetch('/api/categories');
        const json = await res.json();
        if (json.success) {
            state.categories = json.data || [];
            const select = document.getElementById('catalogCategorySelect');
            if (select) {
                select.innerHTML = '<option value="">All Categories & Types</option>' +
                    state.categories.map(c => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`).join('');
            }
        }
    } catch (err) {
        console.error('Error fetching categories', err);
    }
}

async function fetchCatalog() {
    const searchVal = document.getElementById('catalogSearchInput')?.value.trim() || '';
    const catVal = document.getElementById('catalogCategorySelect')?.value || '';
    const availVal = document.getElementById('catalogAvailableOnly')?.checked || false;
    const sortVal = document.getElementById('catalogSortSelect')?.value || 'title';

    const params = new URLSearchParams();
    if (searchVal) params.append('q', searchVal);
    if (catVal) params.append('category', catVal);
    if (availVal) params.append('available', 'true');
    if (sortVal) params.append('sort', sortVal);

    try {
        const res = await fetch('/api/books?' + params.toString());
        const json = await res.json();
        if (json.success) {
            state.catalogItems = json.data || [];
            renderCatalog();
        }
    } catch (err) {
        showToast('Failed to load catalog holdings', 'error');
    }
}

function setCatalogViewMode(mode) {
    state.catalogViewMode = mode;
    localStorage.setItem('athenaeum_view_mode', mode);

    const btnGrid = document.getElementById('btnToggleGrid');
    const btnTable = document.getElementById('btnToggleTable');
    const gridContainer = document.getElementById('catalogGridContainer');
    const tableContainer = document.getElementById('catalogTableContainer');

    if (mode === 'grid') {
        btnGrid.classList.add('active');
        btnTable.classList.remove('active');
        gridContainer.classList.remove('hidden');
        tableContainer.classList.add('hidden');
    } else {
        btnTable.classList.add('active');
        btnGrid.classList.remove('active');
        tableContainer.classList.remove('hidden');
        gridContainer.classList.add('hidden');
    }

    renderCatalog();
}

function renderCatalog() {
    if (state.catalogViewMode === 'grid') {
        renderCatalogGrid();
    } else {
        renderCatalogTable();
    }
}

// Handcrafted Book Cover Tile Generator (Geometric Color Block + Title Initials)
function getCoverClass(genre, type) {
    if (type === 'MAGAZINE') return 'cover-mag';
    if (type === 'JOURNAL') return 'cover-sci';
    if (type === 'REFERENCE') return 'cover-his';
    if (!genre) return 'cover-gen';

    const g = genre.toLowerCase();
    if (g.includes('computer') || g.includes('tech') || g.includes('software')) return 'cover-cs';
    if (g.includes('literature') || g.includes('fiction') || g.includes('classic')) return 'cover-lit';
    if (g.includes('science') || g.includes('physics') || g.includes('biology')) return 'cover-sci';
    if (g.includes('history') || g.includes('biography')) return 'cover-his';
    return 'cover-gen';
}

function getTitleInitials(title) {
    if (!title) return 'BK';
    const words = title.split(/\s+/).filter(w => w.length > 0);
    if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
    return (words[0].charAt(0) + words[1].charAt(0)).toUpperCase();
}

function renderCatalogGrid() {
    const container = document.getElementById('catalogGridContainer');
    if (!container) return;

    if (state.catalogItems.length === 0) {
        container.innerHTML = '<div style="grid-column: 1 / -1; padding: 48px; text-align: center; color: var(--text-muted);"><p>No library holdings matched your query.</p></div>';
        return;
    }

    const isLibrarian = state.currentUser?.role === 'LIBRARIAN';

    container.innerHTML = state.catalogItems.map(item => {
        const coverClass = getCoverClass(item.genre, item.itemType);
        const initials = getTitleInitials(item.title);
        const isAvail = item.availableCopies > 0;
        const canBorrow = item.itemType !== 'REFERENCE';

        return `
            <div class="book-card" id="book-card-${item.id}">
                <!-- Handcrafted Editorial Cover Block -->
                <div class="book-cover-tile ${coverClass}">
                    <div class="cover-badge-row">
                        <span class="cover-type-tag">${item.itemType}</span>
                        <span class="status-pill status-${isAvail ? 'available' : 'overdue'}" style="background-color: rgba(255,255,255,0.2); color: #FFFFFF;">
                            ${isAvail ? item.availableCopies + ' Available' : 'Out of Stock'}
                        </span>
                    </div>
                    <span class="cover-initials">${initials}</span>
                    <div class="cover-title-preview">${escapeHtml(item.title)}</div>
                </div>

                <div class="book-card-body">
                    <h3 class="book-card-title">${escapeHtml(item.title)}</h3>
                    <p class="book-card-author">${escapeHtml(item.author || item.displayDetails || item.publisher)}</p>
                    
                    <div class="book-card-meta">
                        <span><strong>Publisher:</strong> ${escapeHtml(item.publisher)} (${item.year})</span>
                        ${item.isbn ? `<span><strong>ISBN:</strong> ${escapeHtml(item.isbn)}</span>` : ''}
                        ${item.genre ? `<span><strong>Genre:</strong> ${escapeHtml(item.genre)}</span>` : ''}
                    </div>

                    <div class="book-card-footer">
                        <span class="copies-indicator text-muted">${item.availableCopies} of ${item.totalCopies} copies</span>
                        
                        <div style="display: flex; gap: 6px;">
                            ${canBorrow && isAvail ? `
                                <button class="btn btn-primary btn-xs" onclick="openIssueForItem(${item.id})">Issue</button>
                            ` : !isAvail ? `
                                <button class="btn btn-secondary btn-xs" onclick="openReserveForItem(${item.id})">Reserve</button>
                            ` : ''}

                            ${isLibrarian ? `
                                <button class="btn btn-outline btn-xs" onclick="openEditBookModal(${item.id})" title="Edit">✎</button>
                                <button class="btn btn-outline btn-xs" onclick="confirmDeleteBook(${item.id}, '${escapeJs(item.title)}')" title="Delete">🗑</button>
                            ` : ''}
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function renderCatalogTable() {
    const tbody = document.getElementById('catalogTableBody');
    if (!tbody) return;

    if (state.catalogItems.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding: 32px;">No catalog holdings found.</td></tr>';
        return;
    }

    const isLibrarian = state.currentUser?.role === 'LIBRARIAN';

    tbody.innerHTML = state.catalogItems.map(item => {
        const isAvail = item.availableCopies > 0;
        const canBorrow = item.itemType !== 'REFERENCE';

        return `
            <tr>
                <td>
                    <div style="font-weight: 600; color: var(--primary-dark);">${escapeHtml(item.title)}</div>
                    <div style="font-size: 0.76rem; color: var(--text-muted);">${item.publisher}</div>
                </td>
                <td><span class="status-pill status-reserved">${item.itemType}</span></td>
                <td style="font-size: 0.82rem; color: var(--text-secondary);">${escapeHtml(item.author || item.displayDetails || '—')}</td>
                <td>${item.year}</td>
                <td>
                    <span class="status-pill status-${isAvail ? 'available' : 'overdue'}">
                        ${isAvail ? 'Available' : 'Unavailable'}
                    </span>
                </td>
                <td style="font-weight: 600;">${item.availableCopies} / ${item.totalCopies}</td>
                <td class="text-right">
                    <div style="display: inline-flex; gap: 6px;">
                        ${canBorrow && isAvail ? `
                            <button class="btn btn-primary btn-xs" onclick="openIssueForItem(${item.id})">Issue</button>
                        ` : !isAvail ? `
                            <button class="btn btn-secondary btn-xs" onclick="openReserveForItem(${item.id})">Reserve</button>
                        ` : ''}

                        ${isLibrarian ? `
                            <button class="btn btn-outline btn-xs" onclick="openEditBookModal(${item.id})">Edit</button>
                            <button class="btn btn-danger btn-xs" onclick="confirmDeleteBook(${item.id}, '${escapeJs(item.title)}')">Delete</button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// =============================================================================
// ADD / EDIT BOOK MODAL
// =============================================================================
function showAddBookModal() {
    document.getElementById('bookFormId').value = '0';
    document.getElementById('bookModalTitle').textContent = 'Add New Catalog Holding';
    document.getElementById('bookForm').reset();
    document.getElementById('bookItemType').value = 'BOOK';
    toggleHoldingTypeFields();
    openModal('bookModal');
}

function openEditBookModal(id) {
    const item = state.catalogItems.find(b => b.id === id);
    if (!item) return;

    document.getElementById('bookFormId').value = item.id;
    document.getElementById('bookModalTitle').textContent = 'Edit Holding: ' + item.title;
    document.getElementById('bookItemType').value = item.itemType;
    document.getElementById('bookTitle').value = item.title;
    document.getElementById('bookPublisher').value = item.publisher;
    document.getElementById('bookYear').value = item.year;
    document.getElementById('bookCopies').value = item.totalCopies;

    if (item.itemType === 'BOOK') {
        document.getElementById('bookAuthor').value = item.author || '';
        document.getElementById('bookGenre').value = item.genre || '';
        document.getElementById('bookIsbn').value = item.isbn || '';
    }

    toggleHoldingTypeFields();
    openModal('bookModal');
}

function toggleHoldingTypeFields() {
    const type = document.getElementById('bookItemType').value;
    document.querySelectorAll('.subtype-fields').forEach(el => el.classList.add('hidden'));

    if (type === 'BOOK') {
        document.getElementById('fieldsBook').classList.remove('hidden');
    } else if (type === 'MAGAZINE') {
        document.getElementById('fieldsMagazine').classList.remove('hidden');
    } else if (type === 'JOURNAL') {
        document.getElementById('fieldsJournal').classList.remove('hidden');
    } else if (type === 'REFERENCE') {
        document.getElementById('fieldsReference').classList.remove('hidden');
    }
}

async function handleSaveBookSubmit() {
    const id = parseInt(document.getElementById('bookFormId').value);
    const itemType = document.getElementById('bookItemType').value;
    const title = document.getElementById('bookTitle').value.trim();
    const publisher = document.getElementById('bookPublisher').value.trim();
    const year = parseInt(document.getElementById('bookYear').value);
    const totalCopies = parseInt(document.getElementById('bookCopies').value);

    const payload = { itemType, title, publisher, year, totalCopies };

    if (itemType === 'BOOK') {
        payload.author = document.getElementById('bookAuthor').value.trim();
        payload.genre = document.getElementById('bookGenre').value.trim();
        payload.isbn = document.getElementById('bookIsbn').value.trim();
    } else if (itemType === 'MAGAZINE') {
        payload.issueNumber = parseInt(document.getElementById('magIssue').value) || 1;
        payload.month = document.getElementById('magMonth').value.trim();
        payload.category = document.getElementById('magCategory').value.trim();
    } else if (itemType === 'JOURNAL') {
        payload.volume = document.getElementById('jnlVolume').value.trim();
        payload.impactFactor = parseFloat(document.getElementById('jnlImpact').value) || 1.0;
    } else if (itemType === 'REFERENCE') {
        payload.fieldOfStudy = document.getElementById('refDiscipline').value.trim();
        payload.edition = document.getElementById('refEdition').value.trim();
        payload.deweyDecimal = document.getElementById('refDewey').value.trim();
    }

    try {
        const isUpdate = id > 0;
        const url = isUpdate ? `/api/books/${id}` : '/api/books';
        const method = isUpdate ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const json = await res.json();

        if (json.success) {
            closeModal('bookModal');
            showToast(isUpdate ? 'Holding updated' : 'New holding cataloged', 'success');
            await fetchCatalog();
            await fetchDashboardStats();
        } else {
            showToast(json.message || 'Error saving holding', 'error');
        }
    } catch (err) {
        showToast('Network error saving book', 'error');
    }
}

function confirmDeleteBook(id, title) {
    showConfirm(
        'Delete Catalog Holding',
        `Are you sure you wish to permanently remove "${title}" from the repository?`,
        async () => {
            try {
                const res = await fetch(`/api/books/${id}`, { method: 'DELETE' });
                const json = await res.json();
                if (json.success) {
                    showToast('Holding deleted from catalog', 'success');
                    await fetchCatalog();
                    await fetchDashboardStats();
                } else {
                    showToast(json.message || 'Cannot delete item', 'error');
                }
            } catch (err) {
                showToast('Failed to delete holding', 'error');
            }
        }
    );
}

// =============================================================================
// CIRCULATION DESK (ISSUE & RETURN)
// =============================================================================
async function fetchCirculationDesk() {
    await Promise.all([
        populateCirculationPickers(),
        fetchActiveLoans()
    ]);
}

async function populateCirculationPickers() {
    const memberSelect = document.getElementById('deskMemberSelect');
    const bookSelect = document.getElementById('deskBookSelect');
    if (!memberSelect || !bookSelect) return;

    // Members dropdown
    memberSelect.innerHTML = '<option value="">-- Choose Member --</option>' +
        state.members.map(m => `
            <option value="${m.id}" data-quota="${m.activeBorrowCount}" data-max="${m.maxBorrowLimit}">
                ${escapeHtml(m.name)} (${m.membershipId}) — ${m.activeBorrowCount}/${m.maxBorrowLimit} loans
            </option>
        `).join('');

    // Available books dropdown
    const res = await fetch('/api/books?available=true');
    const json = await res.json();
    const availableItems = (json.success ? json.data : []).filter(i => i.itemType !== 'REFERENCE');

    bookSelect.innerHTML = '<option value="">-- Choose Available Item --</option>' +
        availableItems.map(b => `
            <option value="${b.id}">${escapeHtml(b.title)} [${b.availableCopies} available]</option>
        `).join('');
}

function updateMemberQuotaPreview() {
    const select = document.getElementById('deskMemberSelect');
    const preview = document.getElementById('memberQuotaPreview');
    const selectedOption = select.options[select.selectedIndex];

    if (!selectedOption || !selectedOption.value) {
        preview.textContent = 'Select a member to inspect active quota.';
        return;
    }

    const quota = parseInt(selectedOption.getAttribute('data-quota'));
    const max = parseInt(selectedOption.getAttribute('data-max'));

    if (quota >= max) {
        preview.innerHTML = `<span class="text-danger">⚠️ Borrow quota reached (${quota}/${max}). Member cannot check out additional items.</span>`;
    } else {
        preview.innerHTML = `<span class="text-success">✔ Quota healthy: ${quota} of ${max} slots occupied (${max - quota} remaining).</span>`;
    }
}

async function handleDeskIssue() {
    const memberId = parseInt(document.getElementById('deskMemberSelect').value);
    const itemId = parseInt(document.getElementById('deskBookSelect').value);
    const days = parseInt(document.getElementById('deskLoanDays').value) || 14;

    try {
        const res = await fetch('/api/loans/issue', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ itemId, memberId, days })
        });
        const json = await res.json();

        if (json.success) {
            showToast('Item checked out successfully!', 'success');
            document.getElementById('deskIssueForm').reset();
            document.getElementById('memberQuotaPreview').textContent = 'Select a member to inspect active quota.';
            await fetchCirculationDesk();
            await fetchMembers();
            await fetchDashboardStats();
        } else {
            showToast(json.message || 'Issue failed', 'error');
        }
    } catch (err) {
        showToast('Network error issuing item', 'error');
    }
}

async function fetchActiveLoans() {
    try {
        const res = await fetch('/api/loans/active');
        const json = await res.json();
        if (json.success) {
            state.activeLoans = json.data || [];
            renderActiveLoansTable(state.activeLoans);
        }
    } catch (err) {
        console.error('Error fetching active loans', err);
    }
}

function renderActiveLoansTable(loans) {
    const tbody = document.getElementById('activeLoansBody');
    if (!tbody) return;

    if (loans.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted" style="padding: 24px;">No loans currently in circulation.</td></tr>';
        return;
    }

    tbody.innerHTML = loans.map(loan => `
        <tr>
            <td style="font-weight: 600;">${escapeHtml(loan.itemTitle)}</td>
            <td>${escapeHtml(loan.memberName)}</td>
            <td>${loan.dueDate}</td>
            <td><span class="status-pill status-${loan.status.toLowerCase()}">${loan.status}</span></td>
            <td class="text-right">
                <button class="btn btn-primary btn-xs" onclick="processQuickReturn(${loan.id})">Return</button>
                <button class="btn btn-outline btn-xs" onclick="processMarkLost(${loan.id})" title="Mark Lost">Lost</button>
            </td>
        </tr>
    `).join('');
}

function filterActiveLoansTable() {
    const q = document.getElementById('activeLoansFilter').value.trim().toLowerCase();
    const filtered = state.activeLoans.filter(l => 
        l.itemTitle.toLowerCase().includes(q) || 
        l.memberName.toLowerCase().includes(q)
    );
    renderActiveLoansTable(filtered);
}

async function processQuickReturn(txId) {
    try {
        const res = await fetch('/api/loans/return', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ transactionId: txId })
        });
        const json = await res.json();

        if (json.success) {
            showToast('Holding returned and copy restocked', 'success');
            if (state.currentView === 'circulation') fetchCirculationDesk();
            if (state.currentView === 'overdue') fetchOverdueLoans();
            if (state.currentView === 'dashboard') fetchDashboardStats();
        } else {
            showToast(json.message || 'Error processing return', 'error');
        }
    } catch (err) {
        showToast('Network error during book return', 'error');
    }
}

function processMarkLost(txId) {
    showConfirm(
        'Mark Holding as Lost',
        'Marking this item as LOST will decrease the library’s total copy inventory count and close the patron’s loan record. Proceed?',
        async () => {
            try {
                const res = await fetch('/api/loans/lost', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ transactionId: txId })
                });
                const json = await res.json();
                if (json.success) {
                    showToast('Item marked as lost', 'info');
                    fetchCirculationDesk();
                    fetchDashboardStats();
                } else {
                    showToast(json.message || 'Error marking item lost', 'error');
                }
            } catch (err) {
                showToast('Failed to mark item lost', 'error');
            }
        }
    );
}

// Quick Issue Modal Openers
function showQuickIssueModal() {
    populateQuickIssueModal();
    openModal('quickIssueModal');
}

function openIssueForItem(itemId) {
    populateQuickIssueModal(itemId);
    openModal('quickIssueModal');
}

async function populateQuickIssueModal(preselectedItemId = null) {
    const itemSelect = document.getElementById('quickIssueItemSelect');
    const memberSelect = document.getElementById('quickIssueMemberSelect');

    const res = await fetch('/api/books?available=true');
    const json = await res.json();
    const availBooks = (json.success ? json.data : []).filter(i => i.itemType !== 'REFERENCE');

    itemSelect.innerHTML = '<option value="">-- Choose Item --</option>' +
        availBooks.map(b => `<option value="${b.id}" ${b.id === preselectedItemId ? 'selected' : ''}>${escapeHtml(b.title)}</option>`).join('');

    memberSelect.innerHTML = '<option value="">-- Choose Patron --</option>' +
        state.members.map(m => `<option value="${m.id}">${escapeHtml(m.name)} (${m.membershipId})</option>`).join('');
}

async function handleQuickIssueSubmit() {
    const itemId = parseInt(document.getElementById('quickIssueItemSelect').value);
    const memberId = parseInt(document.getElementById('quickIssueMemberSelect').value);
    const days = parseInt(document.getElementById('quickIssueDays').value) || 14;

    try {
        const res = await fetch('/api/loans/issue', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ itemId, memberId, days })
        });
        const json = await res.json();

        if (json.success) {
            closeModal('quickIssueModal');
            showToast('Holding checked out successfully!', 'success');
            await refreshAllData();
            if (state.currentView === 'books') fetchCatalog();
        } else {
            showToast(json.message || 'Issue failed', 'error');
        }
    } catch (err) {
        showToast('Network error during checkout', 'error');
    }
}

// =============================================================================
// OVERDUE & FINES (Multithreaded Daemon)
// =============================================================================
async function fetchOverdueLoans() {
    try {
        const res = await fetch('/api/loans/overdue');
        const json = await res.json();
        if (json.success) {
            state.overdueLoans = json.data || [];
            renderOverduePriorityTable(state.overdueLoans);
        }
    } catch (err) {
        console.error('Error fetching overdue loans', err);
    }
}

function renderOverduePriorityTable(loans) {
    const tbody = document.getElementById('overduePriorityBody');
    if (!tbody) return;

    if (loans.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted" style="padding: 32px;">No overdue loans! All circulating materials are on schedule.</td></tr>';
        return;
    }

    tbody.innerHTML = loans.map(l => `
        <tr>
            <td style="font-family: monospace;">#${l.id}</td>
            <td style="font-weight: 600;">${escapeHtml(l.itemTitle)}</td>
            <td><span class="status-pill status-reserved">${l.itemType}</span></td>
            <td>
                <div><strong>${escapeHtml(l.memberName)}</strong></div>
                <div style="font-size: 0.76rem; color: var(--text-muted);">${escapeHtml(l.memberEmail)}</div>
            </td>
            <td>${l.dueDate}</td>
            <td class="text-danger" style="font-weight: 700;">${l.overdueDays} days late</td>
            <td style="font-size: 1rem; font-weight: 700; color: var(--status-overdue-text);">
                ₹ ${l.fineAmount.toFixed(2)}
            </td>
            <td class="text-right">
                <button class="btn btn-primary btn-xs" onclick="processQuickReturn(${l.id})">Settle & Return</button>
            </td>
        </tr>
    `).join('');
}

async function triggerManualDaemonScan() {
    try {
        const res = await fetch('/api/admin/scan-overdue', { method: 'POST' });
        const json = await res.json();
        if (json.success) {
            showToast('Daemon scan triggered. Overdue fines updated.', 'info');
            await fetchOverdueLoans();
            await fetchDashboardStats();
        }
    } catch (err) {
        showToast('Error triggering scan', 'error');
    }
}

// =============================================================================
// MEMBERS DIRECTORY & BORROWING HISTORY
// =============================================================================
async function fetchMembers() {
    try {
        const res = await fetch('/api/members');
        const json = await res.json();
        if (json.success) {
            state.members = json.data || [];
            renderMembersTable();
        }
    } catch (err) {
        console.error('Error fetching members', err);
    }
}

function renderMembersTable() {
    const tbody = document.getElementById('membersTableBody');
    if (!tbody) return;

    if (state.members.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding: 24px;">No members registered yet.</td></tr>';
        return;
    }

    tbody.innerHTML = state.members.map(m => `
        <tr>
            <td style="font-family: monospace; font-weight: 600;">${escapeHtml(m.membershipId)}</td>
            <td style="font-weight: 600; color: var(--primary-dark);">${escapeHtml(m.name)}</td>
            <td>${escapeHtml(m.email)}</td>
            <td>${escapeHtml(m.phone)}</td>
            <td>
                <span class="status-pill status-${m.activeBorrowCount >= m.maxBorrowLimit ? 'overdue' : 'available'}">
                    ${m.activeBorrowCount} / ${m.maxBorrowLimit} Active
                </span>
            </td>
            <td>${m.joinDate || '—'}</td>
            <td class="text-right">
                <button class="btn btn-outline btn-xs" onclick="openMemberHistory(${m.id}, '${escapeJs(m.name)}')">View Log</button>
            </td>
        </tr>
    `).join('');
}

function showAddMemberModal() {
    document.getElementById('memberForm').reset();
    openModal('memberModal');
}

async function handleSaveMemberSubmit() {
    const name = document.getElementById('memName').value.trim();
    const email = document.getElementById('memEmail').value.trim();
    const phone = document.getElementById('memPhone').value.trim();
    const maxBorrowLimit = parseInt(document.getElementById('memQuota').value) || 3;

    try {
        const res = await fetch('/api/members', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, phone, maxBorrowLimit })
        });
        const json = await res.json();

        if (json.success) {
            closeModal('memberModal');
            showToast('Member registered successfully', 'success');
            await fetchMembers();
            await fetchDashboardStats();
        } else {
            showToast(json.message || 'Error creating member', 'error');
        }
    } catch (err) {
        showToast('Network error registering member', 'error');
    }
}

async function openMemberHistory(memberId, memberName) {
    document.getElementById('historyModalTitle').textContent = 'Borrowing History: ' + memberName;
    const tbody = document.getElementById('historyTableBody');
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Loading history...</td></tr>';
    openModal('historyModal');

    try {
        const res = await fetch(`/api/members/${memberId}/history`);
        const json = await res.json();
        const history = json.success ? json.data : [];

        if (history.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding: 24px;">No recorded transactions for this patron.</td></tr>';
            return;
        }

        tbody.innerHTML = history.map(tx => `
            <tr>
                <td style="font-family: monospace;">#${tx.id}</td>
                <td style="font-weight: 600;">${escapeHtml(tx.itemTitle)}</td>
                <td>${tx.issueDate}</td>
                <td>${tx.dueDate}</td>
                <td>${tx.returnDate || '—'}</td>
                <td><span class="status-pill status-${tx.status.toLowerCase()}">${tx.status}</span></td>
                <td>${tx.fineAmount > 0 ? `₹ ${tx.fineAmount.toFixed(2)}` : '₹ 0.00'}</td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-danger text-center">Failed to load borrowing history.</td></tr>';
    }
}

// =============================================================================
// HOLD RESERVATIONS QUEUE
// =============================================================================
async function fetchReservations() {
    try {
        const res = await fetch('/api/reservations');
        const json = await res.json();
        if (json.success) {
            state.reservations = json.data || [];
            renderReservationsTable();
        }
    } catch (err) {
        console.error('Error fetching reservations', err);
    }
}

function renderReservationsTable() {
    const tbody = document.getElementById('reservationsTableBody');
    if (!tbody) return;

    if (state.reservations.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding: 28px;">No active waitlist hold reservations.</td></tr>';
        return;
    }

    tbody.innerHTML = state.reservations.map(r => `
        <tr>
            <td style="font-weight: 700; font-family: monospace;">#${r.queuePosition}</td>
            <td style="font-weight: 600; color: var(--primary-dark);">${escapeHtml(r.itemTitle)}</td>
            <td>${escapeHtml(r.memberName)}</td>
            <td style="color: var(--text-muted);">${escapeHtml(r.memberEmail)}</td>
            <td>${r.reservationDate}</td>
            <td><span class="status-pill status-${r.status.toLowerCase()}">${r.status}</span></td>
            <td class="text-right">
                ${r.status === 'WAITING' ? `
                    <button class="btn btn-outline btn-xs" onclick="cancelReservation(${r.id})">Cancel Hold</button>
                ` : '—'}
            </td>
        </tr>
    `).join('');
}

async function openReserveForItem(itemId) {
    if (!state.currentUser) {
        showToast('Please log in to place hold reservations', 'error');
        return;
    }

    showConfirm(
        'Place Hold Reservation',
        'All copies of this item are currently checked out. Would you like to enter the FIFO reservation waitlist?',
        async () => {
            try {
                const res = await fetch('/api/reservations', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ itemId, memberId: state.currentUser.id })
                });
                const json = await res.json();

                if (json.success) {
                    showToast('Hold reservation confirmed! You will be notified when restocked.', 'success');
                    if (state.currentView === 'reservations') fetchReservations();
                } else {
                    showToast(json.message || 'Error placing reservation', 'error');
                }
            } catch (err) {
                showToast('Failed to place reservation', 'error');
            }
        }
    );
}

async function cancelReservation(id) {
    try {
        const res = await fetch(`/api/reservations/${id}/cancel`, { method: 'POST' });
        const json = await res.json();
        if (json.success) {
            showToast('Hold reservation cancelled', 'info');
            fetchReservations();
        }
    } catch (err) {
        showToast('Error cancelling reservation', 'error');
    }
}

// =============================================================================
// ASYNCHRONOUS REPORTS (Multithreading ExecutorService)
// =============================================================================
async function generateAsyncReport(reportType) {
    const titleEl = document.getElementById('reportTitle');
    const badgeEl = document.getElementById('reportThreadBadge');
    const contentEl = document.getElementById('reportContentArea');

    titleEl.textContent = 'Generating ' + reportType + ' report...';
    badgeEl.textContent = 'Thread: Dispatching to Pool...';
    contentEl.innerHTML = '<p class="text-muted">Task sent to Java ExecutorService background worker. Awaiting CompletableFuture resolution...</p>';

    try {
        const res = await fetch(`/api/reports/${reportType}`);
        const json = await res.json();

        if (json.success && json.data) {
            const report = json.data;
            titleEl.textContent = reportType.toUpperCase().replace('-', ' ') + ' REPORT';
            badgeEl.textContent = 'Thread: ' + (report.generatedByThread || 'Worker');

            if (reportType === 'most-borrowed') {
                const items = report.data || [];
                contentEl.innerHTML = `
                    <table class="data-table">
                        <thead><tr><th>Rank</th><th>Holding Title</th><th class="text-right">Circulation Count</th></tr></thead>
                        <tbody>
                            ${items.map((it, idx) => `
                                <tr>
                                    <td><strong>#${idx + 1}</strong></td>
                                    <td>${escapeHtml(it.title)}</td>
                                    <td class="text-right" style="font-weight: 700; color: var(--primary);">${it.borrowCount} borrows</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `;
            } else if (reportType === 'overdue-audit') {
                contentEl.innerHTML = `
                    <div style="margin-bottom: 16px;">
                        <p><strong>Total Delinquent Accounts:</strong> ${report.overdueCount || 0}</p>
                        <p><strong>Cumulative Outstanding Penalties:</strong> ₹ ${(report.totalOutstandingFines || 0).toFixed(2)}</p>
                    </div>
                    <table class="data-table">
                        <thead><tr><th>Holding</th><th>Borrower</th><th>Days Late</th><th>Fine</th></tr></thead>
                        <tbody>
                            ${(report.data || []).map(l => `
                                <tr>
                                    <td>${escapeHtml(l.itemTitle)}</td>
                                    <td>${escapeHtml(l.memberName)}</td>
                                    <td class="text-danger">${l.overdueDays} days</td>
                                    <td style="font-weight: 700;">₹ ${l.fineAmount.toFixed(2)}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `;
            } else {
                contentEl.innerHTML = `
                    <pre style="background: var(--bg-surface-alt); padding: 16px; border-radius: 4px; overflow-x: auto; font-size: 0.82rem;">${JSON.stringify(report.data, null, 2)}</pre>
                `;
            }
        } else {
            contentEl.innerHTML = `<p class="text-danger">${json.message || 'Failed generating report'}</p>`;
        }
    } catch (err) {
        contentEl.innerHTML = '<p class="text-danger">Network error during report dispatch</p>';
    }
}

// =============================================================================
// MODAL CONTROLS & CONFIRMATION DIALOGS
// =============================================================================
function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.remove('hidden');
}

function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.add('hidden');
}

function closeAllModals() {
    document.querySelectorAll('.modal-backdrop').forEach(m => m.classList.add('hidden'));
}

function showConfirm(title, message, onConfirm) {
    document.getElementById('confirmModalTitle').textContent = title;
    document.getElementById('confirmModalMessage').textContent = message;

    const btn = document.getElementById('btnConfirmAction');
    // Replace element to purge previous click listeners
    const newBtn = btn.cloneNode(true);
    btn.parentNode.replaceChild(newBtn, btn);

    newBtn.addEventListener('click', () => {
        closeModal('confirmModal');
        if (typeof onConfirm === 'function') onConfirm();
    });

    openModal('confirmModal');
}

// =============================================================================
// TOAST NOTIFICATIONS
// =============================================================================
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;

    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(8px)';
        setTimeout(() => toast.remove(), 200);
    }, 3200);
}

// Utility: HTML Escaping to prevent XSS
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function escapeJs(str) {
    if (!str) return '';
    return String(str).replace(/'/g, "\\'");
}
