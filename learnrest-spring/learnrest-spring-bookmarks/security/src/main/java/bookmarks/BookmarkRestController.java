package bookmarks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookmarks")
class BookmarkRestController {

    private final BookmarkRepository bookmarkRepository;

    private final AccountRepository accountRepository;

    @Autowired
    BookmarkRestController(BookmarkRepository bookmarkRepository,
                           AccountRepository accountRepository) {

        System.out.println("Creating repo");


        this.bookmarkRepository = bookmarkRepository;
        this.accountRepository = accountRepository;
    }

    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, "application/hal+json"})
    Resources<BookmarkResource> readBookmarks(Principal principal) {
        this.validateUser(principal);
        return new Resources<>(this.bookmarkRepository.findByAccountUsername(principal.getName())
                .stream()
                .map(BookmarkResource::new)
                .collect(Collectors.toList()));

    }

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<?> add(Principal principal, @RequestBody Bookmark input) {
        this.validateUser(principal);

        return this.accountRepository
                .findByUsername(principal.getName())
                .map(account -> {
                    Bookmark bookmark = bookmarkRepository.save(new Bookmark(account,
                            input.getUri(), input.getDescription()));

                    Link forOneBookmark = new BookmarkResource(bookmark).getLink("self");

                    return ResponseEntity.created(URI.create(forOneBookmark.getHref())).build();
                })
                .orElse(ResponseEntity.noContent().build());

    }

    @RequestMapping(method = RequestMethod.GET, value = "/{bookmarkId}")
    BookmarkResource readBookmark(Principal principal, @PathVariable Long bookmarkId) {
        this.validateUser(principal);
        return new BookmarkResource(this.bookmarkRepository.findOne(bookmarkId));
    }

    private void validateUser(Principal principal) {
        this.accountRepository.findByUsername(principal.getName()).orElseThrow(
                () -> new UserNotFoundException(principal.getName()));
    }
}
