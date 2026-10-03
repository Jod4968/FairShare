# FairShare

> **Making shared living simpler.**

FairShare is an AI-powered expense-sharing application built for people living together in hostels, PGs, apartments, and shared rooms.

It helps groups **record expenses, split costs, track balances, and settle up** without the usual confusion around who paid, who owes whom, and how much.

The long-term goal is to make expense management as simple as saying:

> "I paid ₹900 for dinner for me, Rahul and Kunal."

FairShare can turn that natural-language request into a structured expense while keeping all financial calculations and business rules deterministic on the backend.

---

## ✨ Why FairShare?

Shared living often means shared expenses:

- 🍕 Food and dinners
- 🛒 Groceries
- 📶 Wi-Fi
- ⚡ Electricity
- 🏠 Rent
- 🚕 Transportation
- 🎮 Entertainment
- 💸 Other shared expenses

Manually calculating these expenses becomes annoying very quickly.

FairShare aims to provide a single place to:

**Record → Split → Track → Settle**

---

## 🚀 Core Features

### Authentication

- User registration
- User login
- JWT-based authentication
- Protected API endpoints
- Authenticated user profile
- Password hashing

### Expense Sharing

Planned core functionality:

- Create and join groups
- Add shared expenses
- Equal expense splitting
- Custom expense splitting
- Expense history
- Track individual balances
- Generate simplified settlement suggestions
- Record completed settlements

### 🤖 AI-Powered Interaction

FairShare is designed to use an open-weight AI model as part of the application.

#### Local assistant setup

Phase 5 uses Gemma through Ollama behind an `AiClient` abstraction. The model only extracts
structured intent; authentication, member resolution, financial calculations, validation, and
database mutations remain in the deterministic Spring services. Because the model runs locally,
financial data does not need to be sent to a proprietary hosted AI API, and the provider can be
inspected or replaced. “Open-weight” is used deliberately; model licensing should be checked
separately for each configured Gemma release.

Install Ollama, pull the configured model, and start it:

```text
ollama pull gemma3:4b
ollama serve
```

Configure the backend with:

```text
AI_ENABLED=true
AI_PROVIDER=ollama
AI_BASE_URL=http://localhost:11434
AI_MODEL=gemma3:4b
```

AI is disabled by default, and FairShare continues to start and support manual functionality
when Ollama is unavailable.

Users will eventually be able to interact with expenses using natural language, for example:

```text
I paid ₹900 for dinner for me, Rahul and Kunal.