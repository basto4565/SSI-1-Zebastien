package sit.project.projectv1.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sit.project.projectv1.entities.Announcement;
import sit.project.projectv1.enums.Mode;
import sit.project.projectv1.enums.Display;
import sit.project.projectv1.repositories.AnnouncementRepository;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {
    @Autowired
    private AnnouncementRepository announcementRepository;
    @Autowired
    private CategoryService categoryService;

    public Announcement getAnnouncementById(Integer announcementId) {
        return announcementRepository.findById(announcementId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement id " + announcementId + " does not exist"));
    }

    public void deleteAnnouncementById(Integer announcementId) {
        if (announcementRepository.existsById(announcementId)) {
            announcementRepository.deleteById(announcementId);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement id " + announcementId + " does not exist");
        }
    }

    public Announcement createAnnouncement(Announcement announcement) {
        return announcementRepository.saveAndFlush(announcement);
    }

    public Announcement updateAnnouncement(Integer announcementId, Announcement announcement) {
        Announcement ann = announcementRepository.findById(announcementId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement id " + announcementId + " does not exist"));
        ann.setAnnouncementTitle(announcement.getAnnouncementTitle());
        ann.setAnnouncementDescription(announcement.getAnnouncementDescription());
        ann.setPublishDate(announcement.getPublishDate());
        ann.setCloseDate(announcement.getCloseDate());
        ann.setAnnouncementDisplay(announcement.getAnnouncementDisplay());
        ann.setAnnouncementCategory(announcement.getAnnouncementCategory());
        return announcementRepository.saveAndFlush(ann);
    }

    public List<Announcement> getAnnouncementList(Mode mode, Integer categoryId) {
        List<Announcement> announcementListAdmin;
        List<Announcement> announcementList;
        Comparator<Announcement> byIdDescending = Comparator.comparingInt(Announcement::getId).reversed();

        if (categoryId.equals(0)) {
                announcementList = announcementRepository.findAll().stream()
                        .filter(a -> a.getAnnouncementDisplay() == Display.Y)
                        .collect(Collectors.toList());
        } else {
                announcementList = announcementRepository.findAllByAnnouncementCategory(categoryService.getCategory(categoryId)).stream()
                        .filter(a -> a.getAnnouncementDisplay() == Display.Y)
                        .collect(Collectors.toList());
        }

        if (mode == Mode.active) {
            List<Announcement> announcementActive = new ArrayList<>();
            checkDateActive(announcementList, announcementActive);
            announcementActive.sort(byIdDescending);
            return announcementActive;
        } else if (mode == Mode.close) {
            List<Announcement> announcementClose = new ArrayList<>();
            checkDateClose(announcementList, announcementClose);
            announcementClose.sort(byIdDescending);
            return announcementClose;
        }
        
        if (categoryId.equals(0)) {
            announcementListAdmin = announcementRepository.findAll();
        } else {
            announcementListAdmin = announcementRepository.findAllByAnnouncementCategory(categoryService.getCategory(categoryId));
        }
        announcementListAdmin.sort(byIdDescending);
        return announcementListAdmin;
    }

    public Page<Announcement> getAnnouncementPage(int page, int size, Mode mode, Integer categoryId) {
        List<Announcement> announcementList;
        Comparator<Announcement> byIdDescending = Comparator.comparingInt(Announcement::getId).reversed();
        Sort sort = Sort.by("id").descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        if (categoryId.equals(0)) {
            announcementList = announcementRepository.findAll().stream()
                    .filter(a -> a.getAnnouncementDisplay() == Display.Y)
                    .collect(Collectors.toList());
        } else {
            announcementList = announcementRepository.findAllByAnnouncementCategory(categoryService.getCategory(categoryId)).stream()
                    .filter(a -> a.getAnnouncementDisplay() == Display.Y)
                    .collect(Collectors.toList());
        }

        if (mode == Mode.active) {
            List<Announcement> announcementActive = new ArrayList<>();
            checkDateActive(announcementList, announcementActive);
            announcementActive.sort(byIdDescending);
            int start = (int) pageRequest.getOffset();
            if (start > announcementActive.size()) {
                return new PageImpl<>(new ArrayList<>(), pageRequest, announcementActive.size());
            }
            int end = Math.min((start + pageRequest.getPageSize()), announcementActive.size());
            return new PageImpl<>(announcementActive.subList(start, end), pageRequest, announcementActive.size());
        } else if (mode == Mode.close) {
            List<Announcement> announcementClose = new ArrayList<>();
            checkDateClose(announcementList, announcementClose);
            announcementClose.sort(byIdDescending);
            int start = (int) pageRequest.getOffset();
            if (start > announcementClose.size()) {
                return new PageImpl<>(new ArrayList<>(), pageRequest, announcementClose.size());
            }
            int end = Math.min((start + pageRequest.getPageSize()), announcementClose.size());
            return new PageImpl<>(announcementClose.subList(start, end), pageRequest, announcementClose.size());
        }

        if (categoryId.equals(0)) {
            return announcementRepository.findAll(pageRequest);
        }
        return announcementRepository.findAllByAnnouncementCategory(pageRequest, categoryService.getCategory(categoryId));
    }

    public void checkDateActive(List<Announcement> announcementList, List<Announcement> announcementActive) {
        ZonedDateTime now = ZonedDateTime.now();
        announcementList.forEach(announcement -> {
            if (announcement.getPublishDate() == null && announcement.getCloseDate() == null) {
                announcementActive.add(announcement);
            } else if (announcement.getPublishDate() != null && announcement.getCloseDate() == null) {
                if (now.compareTo(announcement.getPublishDate()) > 0 || now.compareTo(announcement.getPublishDate()) == 0) {
                    announcementActive.add(announcement);
                }
            } else if (announcement.getPublishDate() != null && announcement.getCloseDate() != null) {
                if ((now.compareTo(announcement.getPublishDate()) > 0 || now.compareTo(announcement.getPublishDate()) == 0) &&
                        now.compareTo(announcement.getCloseDate()) < 0) {
                    announcementActive.add(announcement);
                }
            } else if (announcement.getPublishDate() == null && announcement.getCloseDate() != null) {
                if (now.compareTo(announcement.getCloseDate()) < 0) {
                    announcementActive.add(announcement);
                }
            }
        });
    }

    public void checkDateClose(List<Announcement> announcementList, List<Announcement> announcementClose) {
        ZonedDateTime now = ZonedDateTime.now();
        announcementList.forEach(announcement -> {
            if (announcement.getCloseDate() != null) {
                if ((now.compareTo(announcement.getCloseDate()) > 0 || now.compareTo(announcement.getCloseDate()) == 0)) {
                    announcementClose.add(announcement);
                }
            }
        });
    }
}
