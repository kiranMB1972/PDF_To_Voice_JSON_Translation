@echo off
rem Optional and free: the local translation engine (Ollama). Install from https://ollama.com/download first.
rem Without it the app still works - it reads the ORIGINAL text aloud and tells you the engine is offline.
echo Downloading the translation model once (about 5 GB) ...
ollama pull llama3.1
echo Starting the translation engine ... keep this window open.
ollama serve
pause
