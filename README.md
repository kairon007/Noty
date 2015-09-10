# Noty
Android App to keep track of Notes in Dropbox

# Todo
Add caching to UI to not have to get meta data for every folder again
Push Content aside when drawer is open
Use Snackbar
Change Behaviour of Back-Button
Add transitions

# Efficiency
Don't recursively troll the metadata to build your directory UI on startup. Instead, only get the directory the user is viewing, and do some caching so you don't have to ask constantly.
Use the hash parameter on metadata calls using the previous hash returned so that it returns a shorter list. metadata will give you a 304 if a hash matches and will return the contents again if the hash is different.
Don't download anything until the user has actually asked for it. Keep the hash we give you in the rev entry from the metadata response for the file. Compare rev entries to a fresh metadata call to see if you need to pull the file again or use /delta for a list of instructions to get your local state to match Dropbox's remote state.
