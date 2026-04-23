# Net Worth Tracker - Functional Specification 📊

This document provides a highly granular map of the application's functionality, screen contents, and navigation menus. It is designed to allow a developer on any platform to recreate the user experience with exact precision.

---

## 🏗️ 1. Navigation Architecture

The application is structured into four main functional sections, typically accessed via a **Side Navigation Drawer (Sidebar)**.

### 1.1 Navigation Menu Map

| Section | Menu Item | Purpose |
| :--- | :--- | :--- |
| **Overview** | **Month Overview** | The primary dashboard showing monthly wealth snapshots. |
| | **Net Worth Goals** | View and manage long-term financial targets (1/3/5 years). |
| **Analytics** | **Wealth Graph** | A visual timeline of cumulative net worth over time. |
| | **Allocation** | A portfolio breakdown view (Treemap/Pie). |
| | **Asset Trends** | Individualized historical tracking for specific assets. |
| **Data Management** | **Export** | Generate a portable CSV backup to local device memory. |
| | **Import** | Restore data from a local backup file or previously exported JSON. |
| | **Clear All Data**| Permanent deletion of local and cloud-synced transactional records. |
| **Other** | **Settings** | Configuration for Currency, Number Formatting, and Application Theme. |
| | **Sync to Drive** | Authentication trigger for Google Drive cloud persistence. |

---

## 📱 2. Screen-by-Screen Content Breakdown

### 2.1 Main Dashboard / Portfolio View
The landing screen for daily interaction. It is organized into three distinct vertical zones.

#### A. Interactive Header (Monthly Summary)
- **Primary Data Point**: **Total Net Worth** (e.g., "$1,234,567").
- **Secondary Data**: 
    - **Monthly Delta**: Absolute change compared to the previous month (e.g., "+$5,400").
    - **Growth Percentage**: Relative change (e.g., "+0.45%").
    - **Sparkline Trend**: A 6-month historical line graph showing progress at a glance.
- **Summary Labels**: Displays counts of "Assets" vs "Liabilities" (e.g., "12 assets • 3 liabilities").
- **Monthly Note**: A text preview of any commentary added for the current month.

#### B. Asset / Liability List
- **Existing Entry Row**:
    - **Name**: User-defined label.
    - **Value**: Current monetary value (colored green for assets, red for liabilities).
    - **Delta Indicator**: An arrow (up/down) showing change compared to the same asset in the previous month.
- **Empty State Action**: A large prompt to "Copy all from [Previous Month]" if the current month holds no data.
- **Context Menu (Long Press)**: Options to **Delete Item** or **View specific Trend**.

#### C. Floating Action Controls
- **FAB (Main)**: Expands into a speed-dial on tap.
- **FAB (Sub-Action 1)**: "Add Asset" - Opens the entry bottom-sheet.
- **FAB (Sub-Action 2)**: "Add Note" - Launches the full-screen markdown text editor for the month.

### 2.2 Goal Tracking Screen
A persistent view showing status against user-defined wealth milestones.

| UI Element | Type | Data/Logic Shown |
| :--- | :--- | :--- |
| **Current Worth Bar** | Progress | A read-only display of the current month's consolidated net worth. |
| **1-Year Target Row** | Input/Visual | Progress bar toward 1-year Goal. Shows "% Achieved" and success label. |
| **3-Year Target Row** | Input/Visual | Progress bar toward 3-year Goal. |
| **5-Year Target Row** | Input/Visual | Progress bar toward 5-year Goal. |
| **CAGR Feasibility Hint**| Text | A calculated growth rate requirement (e.g., "Moderate growth of 12% required"). |

### 2.3 Analytics Hub
Dedicated screens for deep-dive financial analysis.
- **History Graph**: A full-screen line chart with time-range toggles (3M, 6M, 1Y, ALL). Shows "Growth" as an area-filled curve.
- **Portfolio Allocation**: A Treemap visualization. Box size correlates to the absolute value of the asset relative to the total portfolio.
- **Individual Trends**: A multi-select UI where users can pick 1 or more specific assets (e.g., "HDFC Bank") to see their specific value progression over time.

---

## 🔢 3. Business Logic & Formula Matrix

| Operation | Logic / Formula |
| :--- | :--- |
| **Consolidation** | $\text{Total} = \sum(\text{Positive Assets}) - \sum(\text{Liabilities})$ |
| **Snapshot Delta** | $\text{Delta} = \text{Snap}_{current} - \text{Snap}_{previous}$ |
| **Growth %** | $(\text{Delta} / \text{Snap}_{previous}) \times 100$ |
| **Goal Feasibility** | $CAGR = (Target / Current)^{1/n} - 1$ |
| **Asset Linking** | Items are linked across months using the `name` field (Unique Identifier = `name + month + year`). |

---

## ⚙️ 4. Global Settings & Metadata

The following parameters define the application's global state and UI behavior.

- **Currency Tokens**: Support for ₹, $, €, £, ¥. Prefixes added to all value strings.
- **Formatting Engine**: Toggles between Indian (12,34,567) and International (1,234,567) grouping.
- **Theme Awareness**: Dark Mode / Light Mode support based on system-level settings or user choice.
- **Sync Protocol**: Push logic sends a JSON payload to Google Drive under the file name `assets.json` in the dedicated AppData folder.

---

> [!IMPORTANT]
> This functional map provides all the "What" and "Where" needed for development. For the "How" (Visual CSS and Tokens), please refer to the [Interactive Design Prototype](designs/project/Net%20Worth%20Tracker.html) in the repository.
