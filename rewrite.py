names = [
    b"\xe6\xb2\x89\xe9\xbb\x98\xe7\x8e\x8b\xe4\xba\x8c",
    b"YiHui",
]

if commit.author_name in names:
    commit.author_name = b"ygrowly"
    commit.author_email = b"lyg3044good@gmail.com"

if commit.committer_name in names:
    commit.committer_name = b"ygrowly"
    commit.committer_email = b"lyg3044good@gmail.com"
