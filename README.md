Setup Instructions
1. Clone the Repository
  git clone <YOUR_REPO_URL>
  cd <YOUR_REPO_FOLDER>
  
2. Start Elasticsearch (via Docker)
  docker-compose up -d
  Runs a single-node Elasticsearch on:
  http://localhost:9200
  curl http://localhost:9200

3. Build & Run the Spring Boot Application
  ./mvnw clean install
  ./mvnw spring-boot:run
   
  On startup, the app:
    Connects to localhost:9200
    Reads sample-courses.json
    Bulk-indexes all course documents into the courses index

API Usage
  Search Courses
  Endpoint:
  
  GET /api/courses/search
Query Parameters:

Parameter	Type	Description
  keyword	String	Search keyword
  userAge	int	Minimum age filter
  category	String	Course category
  type	String	ONE_TIME, COURSE, CLUB
  minPrice	double	Minimum price
  maxPrice	double	Maximum price
  startDate	date	ISO-8601 start date filter
  sort	String	upcoming | priceAsc | priceDesc
  page	int	Page number (default 1)
  size	int	Results per page (default 10)

Example Request:
GET "http://localhost:8080/api/courses/search?category=technology&userAge=11"
Example Response:
[
    {
        "id": "bd1414ee-a18e-47be-ad0d-43aa934569c9",
        "title": "Mobile App Design",
        "description": "This is a comprehensive course on mobile app design designed by Aarav.",
        "category": "Technology",
        "type": "ONE_TIME",
        "gradeRange": "7th–9th",
        "minAge": 11,
        "maxAge": 15,
        "price": 2785.01,
        "nextSessionDate": "2025-09-18T15:31:38"
    }
]

BONUS FUZZY SEARCH 
Searching for vedc will still match "Vedic".

Example:
curl "http://localhost:8080/api/search?q=dinors"

Configuration
Change Elasticsearch host/port in application.yml if needed:
elasticsearch:
  host: localhost
  port: 9200

  
Technologies Used:-
  Java 17
  Spring Boot 3.x
  Elasticsearch 8.x (Docker)
  Elasticsearch Java API Client
  Jackson
  Maven

Deliverables Checklist
 Elasticsearch Docker setup
 Bulk indexing from JSON
 Filters, pagination, sorting
 README with setup & usage instructions
 BONUS:- implemented the fuzzy search
 Demo video link
