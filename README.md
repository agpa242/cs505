# COVID-19 Management Application by Team Incognito Mode

## Problem Statement
Imagine you work for the Kentucky State Government and were tasked with developing solutions to assist in both the reporting and management of healthcare in relation to the COVID-19 pandemic. Based on the request of Governor Beshear your application should provide real-time reporting, alerting, contact tracing, hospital, and state status.

## Technology Used
- Complex event processor: PySiddhi
- DBMS: OrientDB
- Message Broker: RabbitMQ
- Web Server: Grizzly Web Server
- Language: Java

## Project Design
- DBMS: Utilizing a graph database (OrientDB) for storing data, including contact tracing functions and logic as well as operational functions.
- PySiddhi: A complex event processor handling real-time event streams for reporting and alerting.
- Message Broker: RabbitMQ to receive and insert event streams into the OrientDB database.
- Web Server: Grizzly Web Server for hosting APIs and handling web requests.

## Implementation
- Developed APIs for real-time reporting, alerting, contact tracing, hospital management, reset, and state status.
- Designed a database schema for hospital, patient, testing, and vaccination data using OrientDB.
- Utilized complex event processing (PySiddhi) for real-time reporting and alerting.
- Integrated RabbitMQ as a message broker for receiving and inserting event streams.
- Created functions for summarizing patient status, alerting based on zipcode growth, and contact tracing.

## Application Architecture
In this architecture, a graph database stores hospital, patient, testing, and vaccination data. A message broker system (RabbitMQ) receives live data streams from different sources, which are then sent to the graph database for storage. Real-time reporting queries the graph database for the latest data to generate reports, alerts, and visualizations.

## Features
- Real-time Reporting Functions
- Alerting based on Zipcode Growth
- Contact Tracing Function
- Operating Reporting Functions



