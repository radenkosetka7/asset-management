# Asset Management System

A robust and scalable enterprise asset management system built with Spring Boot, featuring advanced search capabilities, secure authentication, OAuth2 social login, and comprehensive asset tracking.

## 🚀 Features

### Core Functionality
- **Asset Management**: Complete CRUD operations for assets with category-based organization
- **Dynamic Attributes**: Flexible attribute system allowing custom fields per category
- **Image Management**: Multi-image support for assets with secure storage
- **Advanced Search**: Full-text search powered by Elasticsearch
- **Filtering & Pagination**: Complex filtering with attribute-based queries and paginated results
- **Caching**: Redis-based caching for improved performance

### Security & Authentication
- **JWT Authentication**: Secure token-based authentication with access and refresh tokens
- **Sign Out**: Refresh token invalidation via `/api/auth/signout`
- **OAuth2 Social Login**: One-click sign-in with **Google** (OIDC) and **GitHub**
- **Auto-provisioning**: New admin accounts are automatically created on first OAuth2 login
- **SSL/TLS**: HTTPS support with self-signed certificates
- **Password Security**: BCrypt password hashing (password is optional for OAuth2 users)
- **Role-based Access Control**: Admin authentication and authorization

### Technical Features
- **RESTful API**: Well-structured REST endpoints
- **Database Migrations**: Liquibase for version-controlled database changes
- **Containerization**: Docker and Docker Compose for easy deployment
- **Testing**: Comprehensive unit and integration tests with Testcontainers
- **Multi-stage Docker Build**: Optimized Docker images for production

## 🛠️ Technology Stack

### Backend
- **Java 21** - Modern Java features and performance improvements
- **Spring Boot 3.5.7** - Application framework
- **Spring Data JPA** - Database abstraction and ORM
- **Spring Security** - Authentication and authorization
- **Hibernate** - JPA implementation

### Database & Search
- **PostgreSQL 16** - Primary relational database
- **Elasticsearch 8.12.0** - Full-text search and analytics
- **Redis 7** - Caching layer

### Security & Authentication
- **JWT (JSON Web Tokens)** - Token-based authentication
- **JJWT 0.11.5** - JWT library for Java
- **OAuth2 / OIDC** - Social login via Google and GitHub
- **Spring Security OAuth2 Client** - OAuth2 integration

### Build & Deployment
- **Maven** - Dependency management and build tool
- **Docker & Docker Compose** - Containerization
- **Liquibase** - Database version control

### Testing
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **Testcontainers** - Integration testing with real containers
- **H2 Database** - In-memory database for tests

### Utilities
- **Lombok** - Reduce boilerplate code
- **Apache Commons Lang3** - Utility functions

## 📋 Prerequisites

- **Java 21** or higher
- **Maven 3.9+**
- **Docker** and **Docker Compose**
- **Git**
- **Google OAuth2 credentials** (from [Google Cloud Console](https://console.cloud.google.com/))
- **GitHub OAuth App credentials** (from [GitHub Developer Settings](https://github.com/settings/developers))

## 🔧 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/asset_management.git
cd asset_management
```

### 2. Environment Configuration

Create a `.env` file in the project root with the following variables:

```env
# Database
POSTGRES_PASSWORD=your_postgres_password

# Elasticsearch
ELASTICSEARCH_PASSWORD=your_elasticsearch_password

# JWT Tokens
SECRET_TOKEN=your_secret_token_here
REFRESH_TOKEN_SECRET=your_refresh_token_secret_here

# SSL
SSL_PASSWORD=your_ssl_keystore_password

# OAuth2 - Google
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# OAuth2 - GitHub
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
```

> **Note**: Do **not** wrap values in quotes in the `.env` file — Spring Boot reads them literally and quotes will be included in the value, breaking OAuth2.

> **Note**: For production, use strong, randomly generated passwords and secrets.

### 3. OAuth2 Provider Setup

#### Google
1. Go to [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials
2. Create an **OAuth 2.0 Client ID** (Web application)
3. Add authorized redirect URI: `https://localhost:8443/login/oauth2/code/google`
4. Copy the **Client ID** and **Client Secret** into your `.env`

#### GitHub
1. Go to [GitHub Developer Settings](https://github.com/settings/developers) → OAuth Apps → New OAuth App
2. Set **Authorization callback URL**: `https://localhost:8443/login/oauth2/code/github`
3. Copy the **Client ID** and **Client Secret** into your `.env`

### 4. Generate SSL Certificate (if not present)

```bash
keytool -genkeypair -alias mycert -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore src/main/resources/keystore.p12 \
  -validity 3650 -storepass your_ssl_password
```

### 5. Start with Docker Compose

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database on port `5432`
- Elasticsearch on port `9200`
- Redis on port `6379`
- Application on port `8443` (HTTPS)

### 6. Alternative: Run Locally

Start the infrastructure:
```bash
docker-compose up -d postgres elasticsearch redis
```

Run the application:
```bash
mvn clean install
mvn spring-boot:run
```

## 📚 API Documentation

### Base URL
```
https://localhost:8443/api
```

### Authentication

#### Sign In (Username & Password)
```http
POST /api/auth/signin
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl..."
}
```

#### OAuth2 Social Login

Redirect the user's browser to one of the following URLs to initiate the OAuth2 flow:

| Provider | Login URL |
|----------|-----------|
| Google   | `https://localhost:8443/oauth2/authorization/google` |
| GitHub   | `https://localhost:8443/oauth2/authorization/github` |

After a successful login, the user is redirected to the configured frontend callback URL with tokens as query parameters:

```
http://127.0.0.1:4200/oauth2/callback?accessToken=<token>&refreshToken=<token>
```

> **Note**: On first OAuth2 login, a new admin account is automatically provisioned using the user's name and email from the provider.

#### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl..."
}
```

#### Sign Out
```http
POST /api/auth/signout
Content-Type: application/json

{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl..."
}
```

> Invalidates the provided refresh token. Returns `204 No Content`.

### Assets

#### Create Asset
```http
POST /api/assets
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}

asset: {
  "name": "Laptop Dell XPS 15",
  "description": "High-performance laptop",
  "categoryId": 1,
  "attributes": [
    {
      "attributeId": 1,
      "value": "Intel Core i7"
    }
  ]
}
images: [file1.jpg, file2.jpg]
```

#### Get Asset by ID
```http
GET /api/assets/{id}
Authorization: Bearer {accessToken}
```

#### Update Asset
```http
PUT /api/assets/{id}
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

#### Delete Asset
```http
DELETE /api/assets/{id}
Authorization: Bearer {accessToken}
```

#### Search Assets
```http
GET /api/assets/search?query=laptop&page=0&size=10
Authorization: Bearer {accessToken}
```

#### Filter Assets
```http
POST /api/assets/filter?page=0&size=10
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "categoryId": 1,
  "attributes": [
    {
      "attributeId": 1,
      "value": "Intel"
    }
  ]
}
```

### Categories

#### Get All Categories
```http
GET /api/categories
Authorization: Bearer {accessToken}
```

#### Create Category
```http
POST /api/categories
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "name": "Electronics",
  "description": "Electronic devices and equipment"
}
```

#### Get Category by ID
```http
GET /api/categories/{id}
Authorization: Bearer {accessToken}
```

### Attributes

#### Create Attribute
```http
POST /api/attributes
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "name": "Processor",
  "type": "TEXT"
}
```

#### Get All Attributes
```http
GET /api/attributes
Authorization: Bearer {accessToken}
```

### Category Attributes

#### Assign Attribute to Category
```http
POST /api/category-attributes
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "categoryId": 1,
  "attributeId": 1,
  "isRequired": true
}
```

## 🗄️ Database Schema

The application uses Liquibase for database migrations. Key entities include:

- **Assets**: Main asset information
- **Categories**: Asset categorization
- **Attributes**: Dynamic attribute definitions
- **CategoryAttributes**: Attributes assigned to categories
- **AssetAttributeValues**: Attribute values for specific assets
- **AssetImages**: Image metadata for assets
- **Admins**: System administrators — extended with `oauth_provider`, `oauth_subject`, and `email` columns to support social login; `password` is nullable for OAuth2-only accounts
- **RefreshTokens**: Hashed JWT refresh tokens with expiry, linked to an admin

### Migration History

| Script | Description |
|--------|-------------|
| `initial-schema.sql` | Base schema |
| `second-script.sql` | ... |
| `third-script.sql` | ... |
| `fourth-script.sql` | ... |
| `fifth-script.sql` | ... |
| `sixth-script.sql` | Added `refresh_token` table with hashed token & expiry |
| `seventh-script.sql` | Added `oauth_provider`, `oauth_subject`, `email` to `admin`; made `password` nullable; added unique index on `(oauth_provider, oauth_subject)` |

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify -P integration-tests
```

### Test Coverage
The project includes:
- **23 Unit Tests** for service layer logic
- **21 Integration Tests** with Testcontainers
- Controller tests for API endpoints

See [test_documentation.md](test_documentation.md) for detailed test documentation.

## 🏗️ Project Structure

```
asset_management/
├── src/
│   ├── main/
│   │   ├── java/com/example/asset_management/
│   │   │   ├── config/          # SecurityConfig, OAuth2SuccessHandler, JwtAuthenticationFilter, etc.
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── exception/       # Custom exceptions
│   │   │   ├── mapper/          # Entity-DTO mappers
│   │   │   ├── model/           # JPA entities (incl. CustomUserDetails)
│   │   │   ├── repository/      # Data repositories
│   │   │   └── service/         # Business logic (incl. CustomOAuth2UserService, CustomOidcUserService)
│   │   └── resources/
│   │       ├── application.yaml # Application configuration
│   │       └── db/changelog/    # Liquibase migrations
│   └── test/                    # Test classes
├── assets/                      # Uploaded asset images
├── .env                         # Environment variables (never commit this)
├── docker-compose.yml           # Docker Compose configuration
├── Dockerfile                   # Multi-stage Docker build
└── pom.xml                      # Maven dependencies
```

## 🔒 Security Considerations

1. **Environment Variables**: Never commit `.env` file or sensitive credentials
2. **No Quotes in `.env`**: Values must be unquoted — Spring Boot reads them literally
3. **SSL Certificates**: Use proper CA-signed certificates in production
4. **Password Policies**: Implement strong password requirements
5. **Token Expiration**:
   - Access Token: 15 minutes
   - Refresh Token: 5 hours
6. **HTTPS Only**: Application runs on HTTPS by default
7. **OAuth2 Security**: Provider + subject pairs are enforced unique; accounts are auto-provisioned safely
8. **Input Validation**: All inputs are validated and sanitized

## 🚀 Deployment

### Production Considerations

1. **Use External Databases**: Configure external PostgreSQL, Elasticsearch, and Redis
2. **Environment Variables**: Use secure secret management (AWS Secrets Manager, HashiCorp Vault)
3. **SSL Certificates**: Obtain proper SSL certificates from a trusted CA
4. **OAuth2 Redirect URIs**: Update redirect URIs in Google Cloud Console and GitHub to match your production domain
5. **Logging**: Configure centralized logging (ELK Stack, CloudWatch)
6. **Monitoring**: Set up application monitoring (Prometheus, Grafana)
7. **Backup**: Implement regular database backups
8. **Scaling**: Use Kubernetes or similar for container orchestration

### Docker Production Build

```bash
docker build -t asset-management:latest .
docker run -d -p 8443:8443 --env-file .env asset-management:latest
```

## 📝 API Response Format

### Success Response
```json
{
  "id": 1,
  "name": "Asset Name",
  "description": "Asset Description",
  "category": {
    "id": 1,
    "name": "Category Name"
  },
  "images": [
    {
      "id": 1,
      "imageUrl": "/api/assets/images/1"
    }
  ],
  "attributes": [
    {
      "id": 1,
      "attribute": {
        "id": 1,
        "name": "Attribute Name"
      },
      "value": "Attribute Value"
    }
  ]
}
```

### Error Response
```json
{
  "message": "Error message",
  "status": 400,
  "timestamp": "2026-02-24T10:30:00"
}
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- Radenko Šetka - Initial work

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Elasticsearch for powerful search capabilities
- Docker for simplifying deployment
- All contributors and maintainers

## 📞 Support

For support, email radenkosetka7@gmail.com or open an issue in the GitHub repository.

## 🔄 Version History

- **0.0.2-SNAPSHOT** - OAuth2 & Token improvements
  - Google (OIDC) and GitHub OAuth2 social login
  - Auto-provisioning of admin accounts on first OAuth2 login
  - `CustomOAuth2UserService` and `CustomOidcUserService` for provider-specific user handling
  - `OAuth2SuccessHandler` — issues JWT tokens and redirects to frontend callback
  - Sign-out endpoint (`/api/auth/signout`) with refresh token invalidation
  - Hashed refresh token storage (`refresh_token` table)
  - Admin schema extended with `oauth_provider`, `oauth_subject`, `email`; `password` made nullable
  - Environment-variable-driven OAuth2 credentials (no secrets in source code)

- **0.0.1-SNAPSHOT** - Initial release
  - Asset CRUD operations
  - JWT authentication
  - Elasticsearch integration
  - Redis caching
  - Docker support
  - Comprehensive testing

---

**Built with ❤️ using Spring Boot**
