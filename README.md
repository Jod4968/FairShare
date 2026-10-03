FairShare
Making shared living simpler.

FairShare is a shared-living expense management application built for hostels, PGs, apartments, and shared rooms. It helps users record expenses, split costs, track balances, and settle payments within a group.

The project also includes an AI-powered assistant using the open-weight Gemma model through Ollama, allowing users to interact with their expenses using natural language. The AI interprets user requests and converts them into structured actions, while the Spring Boot backend remains responsible for validation, authorization, financial calculations, and database operations.

✨ Features
🔐 User registration, login, and JWT authentication
👥 Create and join shared groups
💰 Record and split expenses
📊 Track individual balances
🤝 Generate settlement suggestions
💸 Record and complete settlements
🤖 Natural-language expense and spending queries using Gemma
🛡️ AI cannot directly modify the database or perform financial calculations
💵 Accurate monetary calculations using integer paise
🛠️ Tech Stack

Frontend: React, TypeScript, Vite
Backend: Java, Spring Boot, Spring Security, JWT
Database: PostgreSQL
AI: Gemma 3 4B via Ollama
Build: Maven, npm
Deployment: Docker-ready

🧠 AI Architecture

The AI follows a controlled flow:

User → React → Spring Boot → Gemma → Structured Intent → Validation → Domain Service → Database

Gemma is used only for understanding natural-language requests. All important business logic, authorization, expense calculations, balance calculations, and settlement operations are handled by the backend.

🚀 Getting Started

Clone the repository and start PostgreSQL, then run the Spring Boot backend and React frontend.

For AI functionality, install Ollama, pull the Gemma model, and enable AI in the backend configuration.

ollama pull gemma3:4b

Then start the backend and frontend normally.

🧪 Testing

The backend includes automated tests covering authentication, groups, expenses, balances, settlements, validation, and AI functionality. The frontend is also verified through the production build.

🎯 Project Goal

FairShare was built around a simple idea: shared expenses should not become complicated. Instead of manually calculating who owes whom, users can record expenses, view accurate balances, and use natural language to interact with their group's finances.
