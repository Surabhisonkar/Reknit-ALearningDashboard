cd frontend
cp .env.example .env.local    # API base URL and Cognito app client settings
npm ci
npm run dev                   # http://localhost:5173
Every VITE_* value is compiled into the browser bundle, so only public client settings belong in that file.
