# HyperX Convert - Frontend

Frontend application for HyperX Convert MVP, built with ReactJS + TypeScript + TailwindCSS + Vite.

## 🚀 Tech Stack

- **Framework**: React 18 + TypeScript
- **Build Tool**: Vite
- **Styling**: TailwindCSS
- **HTTP Client**: Axios
- **Routing**: React Router DOM

## 📁 Project Structure

```
frontend/
├── public/                 # Static assets
├── src/
│   ├── components/        # Reusable components
│   │   ├── ui/           # UI components
│   │   └── layout/       # Layout components
│   ├── pages/            # Page components
│   ├── services/         # API services
│   ├── types/            # TypeScript types
│   ├── utils/            # Utility functions
│   ├── hooks/            # Custom hooks
│   └── App.tsx           # Main App component
├── index.html
├── package.json
├── tailwind.config.js
├── tsconfig.json
└── vite.config.ts
```

## 🛠️ Development Setup

### Prerequisites

- Node.js >= 18.0.0
- npm >= 9.0.0

### Installation

```bash
# Install dependencies
npm install

# Copy environment variables
cp .env.example .env

# Start development server
npm run dev
```

The application will be available at `http://localhost:3000`

## 📝 Available Scripts

```bash
# Development
npm run dev              # Start development server
npm run build            # Build for production
npm run preview          # Preview production build

# Code Quality
npm run lint             # Run ESLint
npm run lint:fix         # Fix ESLint errors
npm run type-check       # Run TypeScript type checking

# Testing
npm run test             # Run tests
npm run test:ui          # Run tests with UI
npm run test:coverage    # Run tests with coverage
```

## 🎨 Design System

### Colors

- **Primary**: Blue (#3b82f6)
- **Secondary**: Slate (#64748b)
- **Success**: Green (#10b981)
- **Error**: Red (#ef4444)
- **Warning**: Yellow (#f59e0b)

### Typography

- **Font Family**: Inter
- **Font Weights**: 300, 400, 500, 600, 700

### Components

Custom Tailwind components are defined in `src/index.css`:

- `.btn-primary` - Primary button style
- `.btn-secondary` - Secondary button style
- `.card` - Card container style

## 🔧 Configuration

### Environment Variables

Create a `.env` file in the frontend directory:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=HyperX Convert MVP
VITE_MAX_FILE_SIZE=100MB
```

### Path Aliases

TypeScript path aliases are configured for cleaner imports:

```typescript
import Component from '@/components/Component'
import { ApiService } from '@/services/api'
import { FileType } from '@/types'
```

## 📱 Features

### File Upload
- Drag & drop interface
- File type validation
- Size limit enforcement
- Progress tracking

### Format Conversion
- Multiple format support
- Real-time status updates
- Error handling
- Download management

### User Interface
- Responsive design
- Loading states
- Error handling
- Accessibility features

## 🚀 Deployment

### Build for Production

```bash
npm run build
```

The build artifacts will be stored in the `dist/` directory.

### Docker Deployment

```dockerfile
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 🧪 Testing Strategy

- **Unit Tests**: Component testing with Vitest
- **Integration Tests**: API integration testing
- **E2E Tests**: User flow testing (planned)

## 📊 Performance

- **Bundle Size**: Optimized with Vite
- **Code Splitting**: Route-based splitting
- **Asset Optimization**: Image and font optimization
- **Caching**: Browser caching strategies

## 🔒 Security

- **XSS Protection**: Sanitized user inputs
- **CSRF Protection**: API token validation
- **File Upload Security**: Type and size validation
- **HTTPS**: SSL/TLS encryption in production

## 🤝 Contributing

1. Follow TypeScript strict mode
2. Use ESLint and Prettier for code formatting
3. Write tests for new features
4. Follow conventional commit messages
5. Update documentation for API changes

## 📞 Support

For technical support or questions about the frontend application, please refer to the main project documentation or contact the development team.