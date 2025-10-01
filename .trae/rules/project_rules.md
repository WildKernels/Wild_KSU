1. When a build is done, always run the following sequence:
   - `git add .`
   - `git commit -m "<your message>"`
   - `git push`

2. Commit messages must be descriptive and explain the change.

3. Never push with untracked or uncommitted files.

4. If the user states that something is not working, **never argue that it is working**.  
   - Always assume the issue exists.  
   - Focus on debugging, fixing, or suggesting alternatives.

5. **Never build locally**.  
   - All builds are executed through GitHub CI/CD.