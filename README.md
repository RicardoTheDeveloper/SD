# Googol – Distributed Web Search Engine

This repository contains the complete implementation of **Googol**, a distributed Web search engine developed for the **Distributed Systems** course. The project combines distributed crawling and indexing with a modern Web-based interface, real-time monitoring, and integration with external REST services.

Googol can be accessed from any modern Web browser and provides the same functionality as a traditional search engine, while internally relying on a scalable distributed architecture.

---

## 📌 Project Overview

Googol is a distributed Web search engine inspired by systems such as Google and Bing. It supports:

* Automatic Web crawling and recursive indexing
* Distributed inverted index storage
* Keyword-based search with relevance ranking
* Real-time system status monitoring
* Integration with external information sources and AI-based analysis

The system exposes all functionality through a **Spring Boot Web application**, while internally relying on **Java RMI**, **UDP multicast**, and **WebSockets** to coordinate distributed components.

---

## 🏗️ System Architecture

The system is composed of several distributed components, each with a well-defined responsibility:

* **Downloaders** – Crawl Web pages, extract text and links, and distribute indexing data
* **Index Storage Barrels** – Store partitions of the inverted index and inbound link information
* **Gateway** – Coordinates requests, aggregates results, and exposes remote services
* **Web Server** – Provides the Web interface and real-time updates to users

### Architecture Sketch

```
                           ┌────────────────────┐
                           │     Web Browser     │
                           │ (User Interface)   │
                           └─────────┬──────────┘
                                     │
                               HTTP / HTTPS
                                     │
                           ┌─────────▼──────────┐
                           │   Spring Boot Web  │
                           │       Server       │
                           │  (MVC + WebSocket) │
                           └─────────┬──────────┘
                                     │
                                   RMI
                                     │
                           ┌─────────▼──────────┐
                           │      Gateway       │
                           │ (RMI Coordinator) │
                           └─────────┬──────────┘
               ┌─────────────────────┼─────────────────────┐
               │                     │                     │
             RMI                   RMI                   RMI
               │                     │                     │
┌──────────────▼──────────────┐ ┌──────────────▼──────────────┐
│   Index Storage Barrel 1     │ │   Index Storage Barrel N     │
│  (Inverted Index + Links)   │ │  (Inverted Index + Links)   │
└──────────────▲──────────────┘ └──────────────▲──────────────┘
               │                     │
               └───────── Multicast (UDP) ──────┘
                             (Text & Metadata)
                                     ▲
                                     │
                         ┌───────────┴───────────┐
                         │      Downloaders       │
                         │   (Web Crawlers)       │
                         └───────────┬───────────┘
                                     │
                               HTTP Requests
                                     │
                           ┌─────────▼──────────┐
                           │  World Wide Web    │
                           │   (Web Pages)      │
                           └────────────────────┘
```

---

## 🌐 Web Interface Features

The Web application was developed using **Spring Boot** and **Thymeleaf**, following the **MVC architectural pattern**.

Users can:

* Submit new URLs for indexing
* Perform keyword-based searches
* View search results ordered by relevance
* See page titles, URLs, and short text citations
* Query inbound links for a given page
* Monitor the system status in real time

All interactions with the distributed backend are handled transparently through **Java RMI**.

---

## ⚡ Real-Time Notifications

The system uses **WebSockets** to provide server-push communication to Web clients.

Real-time information includes:

* Active Index Storage Barrels
* Most frequent search terms
* Average response time per Barrel

Updates are sent only when changes occur, ensuring efficient and responsive monitoring without page refreshes.

---

## 🔗 REST API Integrations

### Hacker News

The application integrates with the **Hacker News API**, allowing users to:

* Fetch and index URLs from top stories that match search terms
* Index all stories submitted by a specific Hacker News user

This expands the scope of indexed content and enriches search results.

### OpenAI

The **OpenAI API** is used to generate **contextual AI-based summaries** for search results. These summaries are based on search terms and the content of retrieved pages, improving result interpretation and usability.

---

## 🔐 Security

The Web server is configured to use **HTTPS** with a self-generated SSL certificate. All HTTP requests are automatically redirected to HTTPS, ensuring secure communication.

---

## 🧪 Testing

Functional and integration testing was performed to validate:

* Distributed indexing and search correctness
* RMI communication between components
* Multicast data distribution
* Web interface functionality
* Real-time WebSocket updates
* REST API integrations

All core system functionalities were verified successfully.

---

## ▶️ How to Run the Project

### Prerequisites

* Java 17 (or compatible)
* Maven
* Internet connection (for crawling and REST APIs)

### Execution Steps

1. Start the **Index Storage Barrels**
2. Start the **Gateway**
3. Start one or more **Downloaders**
4. Run the **Spring Boot Web application**

> ⚠️ A browser security warning may appear due to the use of a self-signed certificate.

---

## 📁 Project Structure

* `controller/` – Spring MVC controllers
* `service/` – Business logic and external integrations
* `model/` – Data models and DTOs
* `config/` – RMI, REST, WebSocket, and server configuration
* `migrations/` – Distributed backend components
* `templates/` – Thymeleaf HTML templates
* `static/` – CSS and JavaScript assets

---

## 📚 Technologies Used

* Java
* Spring Boot
* Thymeleaf
* Java RMI
* UDP Multicast
* WebSockets (STOMP)
* REST APIs
* Hacker News API
* OpenAI API

---

## 👤 Author

* **Ricardo Rafael Dias Gomes Simões**

---

## 📄 License

This project was developed for academic purposes as part of a Distributed Systems course.

